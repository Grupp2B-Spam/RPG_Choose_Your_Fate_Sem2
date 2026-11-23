package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * The body of {@code race-definition.updated}.
 *
 * <p>CharacterManager is expected to cache these rather than ask this service for a race
 * at every character creation. The reasoning is in ADR-017 and is worth repeating here
 * because it is the part a reader of this class would otherwise have to guess: a
 * synchronous lookup would make character creation — a user-facing write — fail whenever
 * this admin-only authoring service is unreachable, in exchange for data that changes
 * perhaps twice in the lifetime of the project.
 *
 * @param startingSceneId the starting scene of {@code startingChapterId}, denormalised so
 *                        that a new character can be placed without a second lookup. It is
 *                        a property of the chapter, not of the race, which is why
 *                        {@code raceVersion} has to be bumped and the race republished
 *                        when a chapter's starting scene changes
 */
public record RaceDefinitionPayload(
        Integer raceId,
        int raceVersion,
        String status,
        String name,
        Integer startingChapterId,
        Integer startingSceneId) {
}
