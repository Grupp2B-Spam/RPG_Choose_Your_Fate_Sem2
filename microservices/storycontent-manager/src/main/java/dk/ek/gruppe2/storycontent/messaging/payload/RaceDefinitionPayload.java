package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * The body of {@code race-definition.updated}.
 *
 * <p>CharacterManager caches these instead of asking us for a race at every character
 * creation. A lookup would make creating a character — something a player waits on — fail
 * whenever this admin-only service is unreachable, to fetch data that changes perhaps
 * twice all term.
 *
 * @param startingSceneId the starting scene of {@code startingChapterId}, copied in here
 *                        so a new character can be placed without a second lookup. It
 *                        belongs to the chapter rather than the race, which is why a race
 *                        has to be republished with a new version when its chapter's
 *                        starting scene changes
 */
public record RaceDefinitionPayload(
        Integer raceId,
        int raceVersion,
        String status,
        String name,
        Integer startingChapterId,
        Integer startingSceneId) {
}
