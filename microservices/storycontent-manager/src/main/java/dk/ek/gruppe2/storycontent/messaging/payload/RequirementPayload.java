package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * One condition on a choice, as published. The fields not belonging to kind are null and
 * are omitted from the JSON, so a consumer reads an absent field rather than a null one.
 */
public record RequirementPayload(
        String kind,
        String flag,
        Integer itemId) {
}
