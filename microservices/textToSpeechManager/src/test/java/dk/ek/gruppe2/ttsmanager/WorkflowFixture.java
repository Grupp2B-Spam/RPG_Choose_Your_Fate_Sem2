package dk.ek.gruppe2.ttsmanager;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.oauth2.jwt.*;
import tools.jackson.databind.json.JsonMapper;

/** Real service/database flow; substitute only the audio store, provider and JWT signature boundary. */
public final class WorkflowFixture {
    public final MutableClock clock = new MutableClock();
    public final JdbcTemplate jdbc;
    public final JobStore jobs;
    public final MemoryAudio audio = new MemoryAudio();
    public final AtomicInteger providerCalls = new AtomicInteger();
    public final List<SpeechSpec> publications = new CopyOnWriteArrayList<>();
    public final SpeechSpec spec = new SpeechSpec(SpeechSpec.hash("A journey", "voice", "model", "mp3_44100_128"),
            "A journey", "voice", "model", "mp3_44100_128");
    public final SpeechPolicy policy = new SpeechPolicy(1000, 5000, "voice", "model", "mp3_44100_128");
    public final SpeechProcessor processor;
    public final SpeechLookup lookup;
    public final JwtSecurity security;
    public final SpeechEvents events;

    public WorkflowFixture() {
        var db = new DriverManagerDataSource("jdbc:h2:mem:tts_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(db);
        jdbc = new JdbcTemplate(db);
        jobs = new JobStore(jdbc, clock, 3, Duration.ofSeconds(120));
        processor = new SpeechProcessor(jobs, audio, requested -> {
            providerCalls.incrementAndGet(); return new byte[]{1, 2, 3};
        });
        lookup = new SpeechLookup(jobs, audio, publications::add);
        JwtDecoder callers = token -> {
            if (!"caller-token".equals(token)) throw new JwtException("Invalid token");
            return Jwt.withTokenValue(token).header("alg", "RS256").subject("player-1")
                    .claim("realm_access", Map.of("roles", List.of("USER"))).build();
        };
        JwtDecoder deliveries = token -> {
            if (!"delivery-token".equals(token)) throw new JwtException("Invalid token");
            return Jwt.withTokenValue(token).header("alg", "RS256").subject("trusted-delivery").build();
        };
        security = new JwtSecurity(callers, deliveries, Set.of("trusted-delivery"), "", "USER", "ADMIN");
        events = new SpeechEvents(processor, policy, security, JsonMapper.builder().build(), jdbc, jobs);
    }

    public String eventJson() {
        return JsonMapper.builder().build().writeValueAsString(Map.of("eventId", "event-1", "eventType", "RecapGenerated",
                "eventVersion", 2, "text", spec.text()));
    }

    public static final class MemoryAudio implements AudioStore {
        public final Map<String, byte[]> objects = new ConcurrentHashMap<>();
        public final AtomicInteger signedUrls = new AtomicInteger();
        @Override public boolean exists(String key) { return objects.containsKey(key); }
        @Override public void put(String key, byte[] audio) { objects.put(key, audio.clone()); }
        @Override public String downloadUrl(String key) {
            return "https://audio.example.test/" + key + "?signature=" + signedUrls.incrementAndGet();
        }
    }

    public static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-27T12:00:00Z");
        public void advance(Duration duration) { now = now.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
