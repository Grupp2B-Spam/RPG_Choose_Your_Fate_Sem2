package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.List;

/**
 * One choice inside ScenePayload.
 *
 * @param beat           the authored sentence describing what happens if this choice is
 *                       taken. The consumer copies it into the player's history
 * @param grantedItemIds derived from the GRANT_ITEM entries in effects, kept for consumers
 *                       written before effects existed
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
