package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.List;

/**
 * One choice inside {@link ScenePayload}.
 *
 * @param beat           the authored sentence describing what happens if this choice is
 *                       taken. SceneManager copies it into {@code player-choice.made}, so
 *                       that the player's path records what happened rather than a
 *                       pointer to content that may later change (ADR-007)
 * @param grantedItemIds derived from the {@code GRANT_ITEM} entries in {@code effects} and
 *                       kept only for consumers written against version 1.0.0 of the
 *                       contract. It is published from one source, not stored twice
 */
public record ChoicePayload(
        Integer choiceId,
        String label,
        String beat,
        Integer destinationSceneId,
        int weight,
        List<Integer> grantedItemIds,
        List<RequirementPayload> requirements,
        List<EffectPayload> effects) {
}
