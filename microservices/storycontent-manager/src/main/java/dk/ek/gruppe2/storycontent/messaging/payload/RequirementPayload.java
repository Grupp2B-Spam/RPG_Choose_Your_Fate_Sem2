package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * One condition on a choice, as published.
 *
 * <p>The fields not belonging to {@code kind} are null and are omitted from the JSON by
 * the global {@code non_null} inclusion setting, so a {@code STORY_FLAG} requirement is
 * published as {@code {"kind":"STORY_FLAG","flag":"festival-access"}} rather than with
 * three explicit nulls. That matters for more than tidiness: a consumer reading
 * {@code itemId} as absent is unambiguous, whereas reading it as {@code null} invites the
 * question of whether the publisher meant "no item" or "unknown".
 */
public record RequirementPayload(
        String kind,
        String flag,
        Integer itemId) {
}
