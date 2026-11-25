package dk.chooseyourfate.manager;

import java.util.Map;

// One event fans out to StoryPathManager and CharacterManager queues.
// Snapshot fields preserve the wording/effects seen when the choice was made.
public record ChoiceMadeEvent(
        String eventId,
        String characterId,
        String sceneId,
        long sceneVersion,
        String sceneTitle,
        String sceneText,
        String choiceId,
        String choiceText,
        String nextSceneId,
        Map<String, Object> effects,
        String occurredAt
) {}
