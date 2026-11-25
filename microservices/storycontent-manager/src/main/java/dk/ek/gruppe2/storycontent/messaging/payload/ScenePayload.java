package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.List;

/**
 * The body of {@code scene-content.updated}.
 *
 * <p>The field names here are the wire format described in
 * {@code microservices/contracts/storycontent-manager.asyncapi.yaml}, which other people
 * are building consumers against. Renaming one is a breaking change, not a refactor —
 * which is why these are records separate from the entities rather than the entities
 * serialised directly. An entity is ours to reshape; this is not.
 *
 * <p>It carries the whole scene, not a notification to come and fetch it. A thin message
 * would force the consumer to call back into this service — the synchronous dependency
 * the split removed — and would race: the consumer could fetch a different version from
 * the one the message described.
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
