package dk.ek.gruppe2.ttsmanager;

import com.linecorp.armeria.common.*;
import com.linecorp.armeria.server.annotation.*;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Knative event sink. Binary and structured CloudEvents carry the unchanged RabbitMQ JSON body. */
public final class SpeechEvents {
    private static final Logger log = LoggerFactory.getLogger(SpeechEvents.class);
    private final SpeechProcessor processor;
    private final SpeechPolicy policy;
    private final JwtSecurity security;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private final JobStore jobs;

    public SpeechEvents(SpeechProcessor processor, SpeechPolicy policy, JwtSecurity security, ObjectMapper json, JdbcTemplate jdbc,
                        JobStore jobs) {
        this.processor = processor; this.policy = policy; this.security = security; this.json = json; this.jdbc = jdbc;
        this.jobs = jobs;
    }

    @Post("/events/recap") @Blocking
    public HttpResponse receive(AggregatedHttpRequest request) {
        try {
            // This is the delivery component's JWT, never a player's JWT copied into a queue event.
            security.requireEventSender(request.headers().get("authorization"));
            JsonNode body = json.readTree(request.contentUtf8());
            String contentType = request.headers().get("content-type", "");
            if (contentType.startsWith("application/cloudevents+json")) {
                if (!"1.0".equals(body.path("specversion").asString()) || body.path("id").asString().isBlank()
                        || body.path("source").asString().isBlank()) throw new IllegalArgumentException("Invalid CloudEvent");
                body = body.has("data_base64") ? json.readTree(Base64.getDecoder().decode(body.path("data_base64").asString())) : body.path("data");
            } else if (!"1.0".equals(request.headers().get("ce-specversion"))
                    || request.headers().get("ce-id", "").isBlank() || request.headers().get("ce-source", "").isBlank()) {
                throw new IllegalArgumentException("A CloudEvent is required");
            }
            process(body);
            return HttpResponse.of(HttpStatus.NO_CONTENT);
        } catch (JwtSecurity.Rejected rejected) {
            return HttpResponse.of(rejected.authenticated() ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException invalid) {
            return HttpResponse.of(HttpStatus.BAD_REQUEST);
        } catch (RuntimeException retry) {
            log.warn("Speech event requires redelivery ({})", retry.getClass().getSimpleName());
            // Non-2xx is essential: delivery infrastructure must retry or dead-letter, not acknowledge success.
            return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void process(JsonNode body) {
        String type = body.path("eventType").asString();
        int version = body.path("eventVersion").asInt();
        if (!("RecapGenerated".equals(type) || "SpeechRequested".equals(type)) || (version != 1 && version != 2)
                || body.path("eventId").asString().isBlank()) throw new IllegalArgumentException("Unsupported speech event");
        SpeechSpec spec;
        if ("RecapGenerated".equals(type)) {
            // Older queued recap events may contain options; TTS alone chooses the settings.
            spec = policy.specification(body.path("text").asString());
        } else {
            String hash = body.path("contentHash").asString();
            if (!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid speech job identity");
            try { spec = jobs.get(hash).spec(); }
            catch (EmptyResultDataAccessException missing) { throw new IllegalArgumentException("Unknown speech job", missing); }
        }
        processor.generate(spec);
    }

    @Get("/health/live") public HttpResponse live() { return HttpResponse.of(HttpStatus.OK); }
    @Get("/health/ready") @Blocking public HttpResponse ready() {
        try { jdbc.queryForObject("SELECT 1", Integer.class); return HttpResponse.of(HttpStatus.OK); }
        catch (RuntimeException unavailable) { return HttpResponse.of(HttpStatus.SERVICE_UNAVAILABLE); }
    }
}
