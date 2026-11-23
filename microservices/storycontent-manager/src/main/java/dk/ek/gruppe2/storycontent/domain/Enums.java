package dk.ek.gruppe2.storycontent.domain;

/**
 * The enumerated vocabularies of this bounded context, kept in one file because they are
 * read together and each is a handful of constants.
 *
 * <p>Every one of them appears in the published AsyncAPI contract, so the constant names
 * are the wire format and cannot be renamed without a breaking change. They are stored as
 * strings rather than ordinals for the same reason: an ordinal makes reordering the enum a
 * silent data corruption.
 */
public final class Enums {

    private Enums() {
    }

    /**
     * Whether a scene is delivered to players.
     *
     * <p>{@code RETIRED} is deliberately not a deletion. Recorded paths reference scenes
     * the player has already been through, and those references must keep resolving.
     */
    public enum SceneStatus {
        PUBLISHED,
        RETIRED
    }

    /**
     * Whether an item or race definition is still offered.
     *
     * <p>Same reasoning as {@link SceneStatus}: a player who holds a retired item must
     * still be able to see it, and a character of a retired race must still be playable.
     */
    public enum DefinitionStatus {
        ACTIVE,
        RETIRED
    }

    /**
     * What an item is.
     *
     * <p>The monolith's enum mixed this with where the item is worn
     * ({@code ARMOR_HEAD}, {@code WEAPON}). The contract separates the two, so this does.
     */
    public enum ItemType {
        EQUIPMENT,
        CONSUMABLE,
        QUEST
    }

    /** Where an equipment item is worn. Set only when the type is {@code EQUIPMENT}. */
    public enum EquipmentSlot {
        HEAD,
        CHEST,
        LEGS,
        MAINHAND
    }

    /**
     * The statistics a character has.
     *
     * <p>Enumerated because the set is small and fixed, and because the monolith's
     * untyped effects name {@code lose_hp} — a statistic that exists nowhere in its
     * schema. Nothing caught that; this enum is what catches it.
     */
    public enum CharacterStat {
        INTELLIGENCE,
        CHARISMA,
        FASHION
    }

    /** What must already be true for a choice to be offered. */
    public enum RequirementKind {
        /** The character holds a named story flag. The name is authored here. */
        STORY_FLAG,
        /** The character holds a particular item. */
        ITEM
    }

    /**
     * What taking a choice does.
     *
     * <p>There is no {@code TRAVEL}: moving to the next scene is
     * {@code choice.destinationSceneId}, which every choice already carries. The monolith
     * stored that fact in both places, and its seed data contains rows where the two
     * disagree.
     */
    public enum EffectKind {
        GRANT_ITEM,
        GRANT_QUEST,
        COMPLETE_QUEST,
        MODIFY_STAT,
        GRANT_FLAG
    }
}
