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
 * <p>It carries the complete scene as the read side needs it rather than a notification
 * to go and fetch it (ADR-005). A thin event would force SceneManager's projector to call
 * back into this service, reintroducing the synchronous dependency the split removes, and
 * would create a race in which the projector fetches a version other than the one the
 * event described.
 *
 * @param sceneVersion incremented on every publication; a consumer must discard a message
 *                     whose version is not newer than what it already holds (ADR-006)
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
