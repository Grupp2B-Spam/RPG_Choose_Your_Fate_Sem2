package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.List;

/**
 * One choice inside {@link ScenePayload}.
 *
 * @param beat           the authored sentence describing what happens if this choice is
 *                       taken. The consumer copies it into the player's history, so that
 *                       history records what happened rather than pointing at content
 *                       that may later change
 * @param grantedItemIds derived from the {@code GRANT_ITEM} entries in {@code effects},
 *                       kept only for consumers written before {@code effects} existed.
 *                       Computed from one source, never stored twice
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
