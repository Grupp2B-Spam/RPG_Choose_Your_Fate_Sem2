package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MIGRATED to StoryContentManager: {@code quest_has_item} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is called {@code quest_reward_item} there. It is the reward <em>definition</em>,
 * where {@code inventory_has_item} on this side records what a player actually
 * holds: the same shape on opposite sides of the boundary, told apart by whether an
 * author or a player wrote the row.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * the {@code rewardItemIds} list on a quest — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "quest_has_item")
public class QuestHasItem {

    @EmbeddedId
    private QuestHasItemId id;

    @ManyToOne
    @MapsId("questId")
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @ManyToOne
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private Item item;

    public QuestHasItem() {}

    public QuestHasItem(Quest quest, Item item) {
        this.quest = quest;
        this.item = item;
        this.id = new QuestHasItemId(quest.getId(), item.getId());
    }

    public QuestHasItemId getId() { return id; }
    public void setId(QuestHasItemId id) { this.id = id; }

    public Quest getQuest() { return quest; }
    public void setQuest(Quest quest) { this.quest = quest; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
