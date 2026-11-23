package dk.ek.gruppe2.storycontent.messaging.payload;

/**
 * One effect of taking a choice, as published.
 *
 * <p>As with {@link RequirementPayload}, only the fields belonging to {@code kind} are
 * set; the rest are omitted from the JSON.
 *
 * @param amount a delta, which may be negative. Deltas commute, which is what lets
 *               CharacterManager consume {@code player-choice.made} as competing consumers
 *               and therefore scale horizontally (ADR-011)
 */
public record EffectPayload(
        String kind,
        Integer itemId,
        Integer questId,
        String stat,
        Integer amount,
        String flag) {
}
