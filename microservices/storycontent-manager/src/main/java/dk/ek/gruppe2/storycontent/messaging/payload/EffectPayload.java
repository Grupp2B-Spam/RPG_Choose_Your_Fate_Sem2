package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * One effect of taking a choice, as published.
 *
 * <p>As with {@link RequirementPayload}, only the fields belonging to {@code kind} are
 * set; the rest are omitted from the JSON.
 *
 * @param amount a delta, which may be negative. Deltas can be applied in any order and
 *               give the same result, which is what lets CharacterManager run several
 *               consumer instances at once
 */
public record EffectPayload(
        String kind,
        Integer itemId,
        Integer questId,
        String stat,
        Integer amount,
        String flag) {
}
