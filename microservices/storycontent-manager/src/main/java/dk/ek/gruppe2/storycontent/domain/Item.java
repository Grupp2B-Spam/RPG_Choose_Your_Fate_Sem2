package dk.ek.gruppe2.storycontent.domain;

import java.util.EnumMap;
import java.util.Map;

import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import dk.ek.gruppe2.storycontent.domain.Enums.EquipmentSlot;
import dk.ek.gruppe2.storycontent.domain.Enums.ItemType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;

/**
 * An item definition: what an item is and what it does, not who holds one.
 *
 * <p>The distinction is the content/state line in miniature. {@code inventory_has_item}
 * on the other side of the boundary has the same shape as the reward tables here; what
 * separates them is whether an author or a player wrote the row.
 *
 * <p>Published as {@code item-definition.updated}. The payload deliberately carries only
 * what a consumer needs in order to enforce rules — type, slot, requirements, modifiers —
 * and not the name or description, which are presentation and are read from the content
 * side where they can be cached.
 */
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemType type;

    /** Set exactly when {@link #type} is {@code EQUIPMENT}; enforced by a check constraint. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private EquipmentSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DefinitionStatus status = DefinitionStatus.ACTIVE;

    @Column(nullable = false)
    private int version = 1;

    /**
     * Minimum statistics required to equip the item.
     *
     * <p>Rows rather than a JSON column, so that the statistic name is constrained. An
     * authored value naming a statistic that does not exist is precisely the failure the
     * monolith could not catch — its data contains {@code lose_hp}, and the schema has no
     * hp.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_requirement", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "stat", length = 16)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "amount", nullable = false)
    private Map<CharacterStat, Integer> requirements = new EnumMap<>(CharacterStat.class);

    /** Effect on a character's statistics while the item is equipped. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_modifier", joinColumns = @JoinColumn(name = "item_id"))
    @MapKeyColumn(name = "stat", length = 16)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "amount", nullable = false)
    private Map<CharacterStat, Integer> modifiers = new EnumMap<>(CharacterStat.class);

    protected Item() {
        // for JPA
    }

    public Item(String name, String description, ItemType type, EquipmentSlot slot) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.slot = slot;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public void setSlot(EquipmentSlot slot) {
        this.slot = slot;
    }

    public DefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(DefinitionStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public Map<CharacterStat, Integer> getRequirements() {
        return requirements;
    }

    public Map<CharacterStat, Integer> getModifiers() {
        return modifiers;
    }
}
