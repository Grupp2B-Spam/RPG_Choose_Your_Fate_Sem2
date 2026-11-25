package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

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
@Embeddable
public class QuestHasItemId implements Serializable {

    private Integer questId;
    private Integer itemId;

    public QuestHasItemId() {}

    public QuestHasItemId(Integer questId, Integer itemId) {
        this.questId = questId;
        this.itemId = itemId;
    }

    public Integer getQuestId() { return questId; }
    public void setQuestId(Integer questId) { this.questId = questId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuestHasItemId that)) return false;
        return Objects.equals(questId, that.questId) &&
                Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questId, itemId);
    }
}
