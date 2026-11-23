package dk.ek.gruppe2.chooseyourfate.model.mysql;


import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import jakarta.persistence.*;

/**
 * MIGRATED to StoryContentManager: {@code item} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>StoryContentManager publishes it as {@code item-definition.updated}. Its schema also
 * separates what an item is from where it is worn — this table's {@code type}
 * column conflates the two — and adds the requirements and modifiers the contract
 * publishes, which do not exist here.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/items} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 45)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private ItemType type;

    public Item() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
}
