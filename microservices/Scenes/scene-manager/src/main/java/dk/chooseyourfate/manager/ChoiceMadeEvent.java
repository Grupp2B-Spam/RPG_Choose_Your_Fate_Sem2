package dk.chooseyourfate.manager;

public record ChoiceMadeEvent(
        String eventId,
        String eventType,
        String occurredAt,
        int schemaVersion,
        PlayerChoicePayload payload
) {
    public record PlayerChoicePayload(
            String characterId,
            String sceneId,
            long sceneVersion,
            String choiceId,
            String choiceLabel,
            String beat,
            String destinationSceneId
    ) {}
}
