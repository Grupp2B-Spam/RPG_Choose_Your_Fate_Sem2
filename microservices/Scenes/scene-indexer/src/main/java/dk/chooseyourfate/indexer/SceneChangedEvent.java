package dk.chooseyourfate.indexer;

import java.util.List;

public record SceneChangedEvent(
        String eventId,
        String eventType,
        String occurredAt,
        int schemaVersion,
        ScenePayload payload
) {
    public record ScenePayload(
            String sceneId,
            long sceneVersion,
            String status,
            String chapterId,
            String name,
            String description,
            List<ContractChoice> choices,
            List<String> npcIds,
            List<String> questIds
    ) {}

    public record ContractChoice(
            String choiceId,
            String label,
            String beat,
            String destinationSceneId,
            Integer weight,
            List<String> grantedItemIds
    ) {}
}
