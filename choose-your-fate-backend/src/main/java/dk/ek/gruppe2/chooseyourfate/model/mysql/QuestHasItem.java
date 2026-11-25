package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MOVED: {@code quest_has_item} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Called {@code quest_reward_item} there. It is the reward an author promised; {@code
 * inventory_has_item} here is what a player actually holds.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — the reward items on a quest.
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
