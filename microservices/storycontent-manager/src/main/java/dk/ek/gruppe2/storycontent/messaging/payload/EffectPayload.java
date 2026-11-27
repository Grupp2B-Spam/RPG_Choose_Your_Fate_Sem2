package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * One effect of taking a choice, as published. Only the fields belonging to kind are set;
 * the rest are omitted from the JSON.
 *
 * @param amount a delta, which may be negative. Deltas can be applied in any order, which
 *               is what lets CharacterManager run several consumer instances
 */
public record EffectPayload(
        String kind,
        Integer itemId,
        Integer questId,
        String stat,
        Integer amount,
        String flag) {
}
