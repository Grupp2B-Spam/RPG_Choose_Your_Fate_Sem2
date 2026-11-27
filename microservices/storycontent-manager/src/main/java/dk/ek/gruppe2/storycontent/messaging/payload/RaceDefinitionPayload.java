package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * The body of race-definition.updated. CharacterManager caches these instead of asking for
 * a race at every character creation, so creating a character does not depend on this
 * admin-only service being reachable.
 *
 * @param startingSceneId the starting scene of startingChapterId, copied in so a new
 *                        character can be placed without a second lookup. It belongs to the
 *                        chapter, which is why a race is republished with a new version
 *                        when its chapter's starting scene changes
 */
public record RaceDefinitionPayload(
        Integer raceId,
        int raceVersion,
        String status,
        String name,
        Integer startingChapterId,
        Integer startingSceneId) {
}
