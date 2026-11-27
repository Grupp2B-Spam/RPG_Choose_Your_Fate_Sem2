package dk.chooseyourfate.indexer;

import java.util.List;

// Full-scene event published by StoryContentManager to game.events (scene.changed).
// version must be monotonically increasing for each sceneId.
public record SceneChangedEvent(
        String id,
        String sceneId,
        long version,
        String operation,  // UPSERT or DELETE
        String chapterId,
        String status,
        String name,
        String description,
        List<Choice> choices,
        List<String> npcIds,
        List<String> questIds,
        String title,
        String text,
) {}
