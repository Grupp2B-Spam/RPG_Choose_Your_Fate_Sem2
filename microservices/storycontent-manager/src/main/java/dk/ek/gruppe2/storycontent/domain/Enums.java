package dk.ek.gruppe2.storycontent.domain;

/**
 * The enumerated vocabularies of this bounded context, kept in one file because they are
 * read together.
 *
 * Every one appears in the published contract, so the constant names are the wire format
 * and cannot be renamed without a breaking change. Stored as strings rather than ordinals
 * for the same reason: an ordinal makes reordering an enum a silent data corruption.
 */
public final class Enums {

    private Enums() {
    }

    /**
     * Whether a scene is delivered to players. RETIRED is not a deletion: recorded paths
     * reference scenes the player has already been through.
     */
    public enum SceneStatus {
        PUBLISHED,
        RETIRED
    }

    /**
     * Whether an item or race definition is still offered. Not a deletion: a player who
     * holds a retired item must still be able to see it.
     */
    public enum DefinitionStatus {
        ACTIVE,
        RETIRED
    }

    /** What an item is. Kept separate from where it is worn. */
    public enum ItemType {
        EQUIPMENT,
        CONSUMABLE,
        QUEST
    }

    /** Where an equipment item is worn. Set only when the type is EQUIPMENT. */
    public enum EquipmentSlot {
        HEAD,
        CHEST,
        LEGS,
        MAINHAND
    }

    /**
     * The statistics a character has. Enumerated so an authored effect cannot name one that
     * does not exist.
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
     * What taking a choice does. There is no TRAVEL: moving to the next scene is
     * choice.destinationSceneId, which every choice already carries.
     */
    public enum EffectKind {
        GRANT_ITEM,
        GRANT_QUEST,
        COMPLETE_QUEST,
        MODIFY_STAT,
        GRANT_FLAG
    }
}
