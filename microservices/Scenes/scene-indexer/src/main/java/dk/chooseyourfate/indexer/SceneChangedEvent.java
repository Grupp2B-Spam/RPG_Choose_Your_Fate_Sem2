package dk.chooseyourfate.indexer;

import java.util.List;

// Full-scene event published by StoryContentManager to game.events (scene.changed).
// version must be monotonically increasing for each sceneId.
public record SceneChangedEvent(
        String sceneId,
        long version,
        String operation,  // UPSERT or DELETE
        String title,
        String text,
        List<Choice> choices
) {}
