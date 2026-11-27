package dk.ek.gruppe2.ttsmanager.connection;

import com.sun.net.httpserver.HttpServer;
import dk.ek.gruppe2.ttsmanager.*;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class ElevenLabsSynthesizerCT {
    private HttpServer server;
    private HttpClient http;
    private URI endpoint;
    private final SpeechSpec spec = new SpeechSpec("unused-by-provider-adapter", "A saved journey",
            "JBFqnCBsd6RMkjVDRZzb", "eleven_turbo_v2_5", "mp3_44100_128");

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
        endpoint = URI.create("http://localhost:" + server.getAddress().getPort());
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    }
    @AfterEach void stop() { server.stop(0); http.close(); }

    @Test void sendsTheExactNarrativeAndOptionsAndReturnsProviderAudio() {
        var requestBody = new AtomicReference<String>();
        var apiKey = new AtomicReference<String>();
        var path = new AtomicReference<String>();
        server.createContext("/v1/text-to-speech/", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("xi-api-key"));
            path.set(exchange.getRequestURI().toString());
            exchange.sendResponseHeaders(200, 3);
            exchange.getResponseBody().write(new byte[]{1, 2, 3});
            exchange.close();
        });
        var provider = provider(100);
        assertThat(provider.synthesize(spec)).containsExactly(1, 2, 3);
        var json = JsonMapper.builder().build().readTree(requestBody.get());
        assertThat(json.path("text").asString()).isEqualTo(spec.text());
        assertThat(json.path("model_id").asString()).isEqualTo(spec.model());
        assertThat(apiKey.get()).isEqualTo("test-provider-key");
        assertThat(path.get()).isEqualTo("/v1/text-to-speech/" + spec.voiceId() + "?output_format=" + spec.outputFormat());
    }

    @Test void providerErrorCannotBecomeCachedAudio() {
        server.createContext("/", exchange -> { exchange.sendResponseHeaders(429, -1); exchange.close(); });
        assertThatThrownBy(() -> provider(100).synthesize(spec)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTP 429");
    }

    @Test void oversizedProviderResponseCannotExhaustTheAudioBuffer() {
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, 4);
            exchange.getResponseBody().write(new byte[]{1, 2, 3, 4});
            exchange.close();
        });
        assertThatThrownBy(() -> provider(3).synthesize(spec)).isInstanceOf(IllegalStateException.class);
    }

    private ElevenLabsSynthesizer provider(int limit) {
        return new ElevenLabsSynthesizer(http, endpoint, "test-provider-key", Duration.ofSeconds(2),
                limit, JsonMapper.builder().build());
    }
}
