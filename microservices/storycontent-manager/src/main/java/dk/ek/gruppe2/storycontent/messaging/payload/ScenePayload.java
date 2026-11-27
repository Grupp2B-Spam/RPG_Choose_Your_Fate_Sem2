package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.List;

/**
 * The body of scene-content.updated.
 *
 * These field names are the wire format other people are building consumers against.
 * Renaming one is a breaking change, which is why this is a record separate from the
 * entity.
 *
 * It carries the whole scene, not a notification to come and fetch it: a thin message would
 * force the consumer to call back into this service, and would race with the next edit.
 *
 * @param sceneVersion incremented on every publication; a consumer must discard a message
 *                     whose version is not newer than what it already holds
 */
public record ScenePayload(
        Integer sceneId,
        int sceneVersion,
        String status,
        Integer chapterId,
        String name,
        String description,
        List<ChoicePayload> choices,
        List<Integer> npcIds,
        List<Integer> questIds) {
}
