package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

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
