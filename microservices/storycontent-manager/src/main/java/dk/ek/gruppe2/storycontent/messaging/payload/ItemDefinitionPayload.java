package dk.ek.gruppe2.storycontent.messaging.payload;

import java.util.Map;

/**
 * The body of item-definition.updated. Carries only what a consumer needs in order to
 * enforce rules — type, slot, requirements and modifiers — not the name or description.
 *
 * @param status RETIRED withdraws the item from being granted or acquired. Not a deletion:
 *               a player who owns one must still be able to see and use it
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
