package dk.ek.gruppe2.chooseyourfate.model.mysql;


import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import jakarta.persistence.*;

/**
 * MOVED: {@code item} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Published as {@code item-definition.updated}. Its schema also splits what an item is
 * from where it is worn, which this {@code type} column mixes together.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/items}.
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
