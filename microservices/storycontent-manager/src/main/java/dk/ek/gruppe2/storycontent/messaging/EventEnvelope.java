package dk.ek.gruppe2.storycontent.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * The outer shape shared by every message in the system.
 *
 * <p>Only {@code payload} differs per event. See {@code microservices/contracts/README.md}.
 *
 * @param eventId       unique per publication; consumers deduplicate on this, because
 *                      delivery is at-least-once
 * @param eventType     identical to the routing key
 * @param occurredAt    when the fact happened, not when it was published
 * @param schemaVersion incremented only on a breaking change to the payload
 * @param payload       the event body
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        int schemaVersion,
        T payload) {

    public static <T> EventEnvelope<T> of(String eventType, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                1,
                payload);
    }
}
