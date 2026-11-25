package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
public interface QuestHasItemRepository extends JpaRepository<QuestHasItem, QuestHasItemId> {
    List<QuestHasItem> findByQuest_Id(Integer questId);
    List<QuestHasItem> findByItem_Id(Integer itemId);
}