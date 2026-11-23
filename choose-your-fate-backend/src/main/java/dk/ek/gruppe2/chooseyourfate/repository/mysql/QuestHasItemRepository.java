package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
public interface QuestHasItemRepository extends JpaRepository<QuestHasItem, QuestHasItemId> {
    List<QuestHasItem> findByQuest_Id(Integer questId);
    List<QuestHasItem> findByItem_Id(Integer itemId);
}