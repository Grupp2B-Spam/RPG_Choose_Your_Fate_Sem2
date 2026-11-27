package dk.ek.gruppe2.ttsmanager;

import io.minio.MinioClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class TtsConfiguration {
    @Bean ObjectMapper json() { return JsonMapper.builder().build(); }
    @Bean Clock clock() { return Clock.systemUTC(); }

    @Bean SpeechPolicy policy(Environment env) {
        String format = required(env, "tts.format");
        if (!format.startsWith("mp3_")) throw new IllegalArgumentException("Only MP3 output is currently supported");
        return new SpeechPolicy(number(env, "tts.max-characters", 1, 100000),
                number(env, "tts.max-wait-millis", 0, 30000), required(env, "tts.voice"), required(env, "tts.model"), format);
    }

    @Bean JobStore jobs(JdbcTemplate jdbc, Clock clock, Environment env) {
        int lease = number(env, "tts.lease-seconds", 150, 600);
        int timeout = number(env, "tts.provider-timeout-seconds", 1, 45);
        if (lease <= timeout + 60) throw new IllegalArgumentException("Lease must exceed bounded provider/storage work");
        return new JobStore(jdbc, clock, number(env, "tts.max-attempts", 1, 10), Duration.ofSeconds(lease));
    }

    @Bean AudioStore audio(Environment env) {
        String region = required(env, "tts.minio-region");
        String access = required(env, "tts.minio-access-key");
        String secret = required(env, "tts.minio-secret-key");
        var http = new OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build();
        var storage = MinioClient.builder().endpoint(httpUri(env, "tts.minio-endpoint").toString())
                .credentials(access, secret).region(region).httpClient(http).build();
        var signer = MinioClient.builder().endpoint(httpUri(env, "tts.minio-public-endpoint").toString())
                .credentials(access, secret).region(region).httpClient(http).build();
        var audio = new MinioAudioStore(storage, signer, required(env, "tts.minio-bucket"), region,
                number(env, "tts.url-lifetime-seconds", 30, 3600));
        if (env.acceptsProfiles(Profiles.of("local"))) audio.initializeBucket();
        return audio;
    }

    @Bean SpeechSynthesizer synthesizer(Environment env, ObjectMapper json, ResourceLoader resources) throws java.io.IOException {
        int limit = number(env, "tts.max-audio-bytes", 1, 52428800);
        if ("mock".equals(required(env, "tts.provider-mode"))) {
            String location = required(env, "tts.mock-audio-file");
            var resource = location.startsWith("classpath:") || location.startsWith("file:")
                    ? resources.getResource(location) : new FileSystemResource(location);
            try (var input = resource.getInputStream()) {
                byte[] fixture = input.readNBytes(limit + 1);
                if (fixture.length == 0 || fixture.length > limit) throw new IllegalArgumentException("Invalid MP3 fixture size");
                return spec -> fixture.clone();
            }
        }
        if (!"elevenlabs".equals(required(env, "tts.provider-mode"))) throw new IllegalArgumentException("Unknown speech provider mode");
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
        return new ElevenLabsSynthesizer(http, httpUri(env, "tts.provider-url"), required(env, "tts.provider-key"),
                Duration.ofSeconds(number(env, "tts.provider-timeout-seconds", 1, 45)), limit, json);
    }

    @Bean @Profile("local") Declarables speechQueues(Environment env) {
        String queueName = required(env, "tts.queue");
        var queue = QueueBuilder.durable(queueName).deadLetterExchange("").deadLetterRoutingKey(queueName + ".dlq").build();
        var dlq = QueueBuilder.durable(queueName + ".dlq").build();
        // The publisher owns the exchange contract. Declare its matching topology locally so
        // Docker/manual queue tests do not require a running RecapManager application.
        var exchange = new TopicExchange(required(env, "tts.exchange"), true, false);
        var binding = new Binding(queueName, Binding.DestinationType.QUEUE, required(env, "tts.exchange"),
                required(env, "tts.routing-key"), Map.of());
        return new Declarables(exchange, queue, dlq, binding);
    }

    @Bean WorkPublisher publisher(RabbitTemplate rabbit, ObjectMapper json, Environment env) {
        rabbit.setMandatory(true);
        return new RabbitWorkPublisher(rabbit, json, required(env, "tts.queue"), number(env, "tts.confirm-timeout-millis", 1, 3000));
    }

    @Bean SpeechProcessor processor(JobStore jobs, AudioStore audio, SpeechSynthesizer provider) {
        return new SpeechProcessor(jobs, audio, provider);
    }
    @Bean SpeechLookup lookup(JobStore jobs, AudioStore audio, WorkPublisher publisher) {
        return new SpeechLookup(jobs, audio, publisher);
    }
    @Bean SpeechRpc rpc(SpeechLookup lookup, SpeechPolicy policy) { return new SpeechRpc(lookup, policy); }
    @Bean SpeechEvents events(SpeechProcessor processor, SpeechPolicy policy, JwtSecurity security, ObjectMapper json, JdbcTemplate jdbc,
                             JobStore jobs) {
        return new SpeechEvents(processor, policy, security, json, jdbc, jobs);
    }

    @Bean @Profile("!local") JwtSecurity security(Environment env) {
        Set<String> subjects = new HashSet<>(Arrays.asList(required(env, "tts.event-subjects").split(",")));
        subjects = subjects.stream().map(String::trim).filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toSet());
        if (subjects.isEmpty()) throw new IllegalArgumentException("An event-delivery subject is required");
        return new JwtSecurity(decoder(env, "tts.jwt-issuer", "tts.jwt-audience", "tts.jwt-jwks"),
                decoder(env, "tts.event-issuer", "tts.event-audience", "tts.event-jwks"), subjects,
                env.getProperty("tts.jwt-client-id", ""), required(env, "tts.jwt-user-role"), required(env, "tts.jwt-admin-role"));
    }

    private JwtDecoder decoder(Environment env, String issuerProperty, String audienceProperty, String jwksProperty) {
        String issuer = httpUri(env, issuerProperty).toString();
        String audience = required(env, audienceProperty);
        String keys = httpUri(env, jwksProperty).toString();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3)); factory.setReadTimeout(Duration.ofSeconds(3));
        var decoder = NimbusJwtDecoder.withJwkSetUri(keys).jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(factory)).build();
        decoder.setJwtValidator(validators(issuer, audience));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> jwt.getExpiresAt() != null && jwt.getSubject() != null
                && !jwt.getSubject().isBlank() && jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), requiredClaims);
    }

    static String required(Environment env, String key) {
        String value = env.getRequiredProperty(key);
        if (value.isBlank()) throw new IllegalArgumentException(key + " must be configured");
        return value;
    }
    static int number(Environment env, String key, int min, int max) {
        int value = env.getRequiredProperty(key, Integer.class);
        if (value < min || value > max) throw new IllegalArgumentException(key + " outside supported limits");
        return value;
    }
    static URI httpUri(Environment env, String key) {
        URI uri = URI.create(required(env, key));
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getUserInfo() != null) {
            throw new IllegalArgumentException(key + " must be an HTTP(S) URL without credentials");
        }
        return uri;
    }
}
