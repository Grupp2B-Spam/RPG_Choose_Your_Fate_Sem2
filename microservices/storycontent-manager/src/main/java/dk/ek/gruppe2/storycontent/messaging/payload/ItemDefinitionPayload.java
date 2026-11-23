package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.Map;

/**
 * The body of {@code item-definition.updated}.
 *
 * <p>Carries only what a consumer needs in order to enforce rules — type, slot,
 * requirements and modifiers — and deliberately not the name or description. Those are
 * presentation, they are read from the content side, and they are static and cacheable;
 * putting them here would mean republishing every item whenever a writer fixed a typo.
 *
 * @param status {@code RETIRED} withdraws the item from being granted or acquired. It is
 *               not a deletion: a player who owns a retired item must still be able to see
 *               and use it, so existing references have to keep resolving
 */
public record ItemDefinitionPayload(
        Integer itemId,
        int itemVersion,
        String status,
        String type,
        String slot,
        Map<String, Integer> requirements,
        Map<String, Integer> modifiers) {
}
