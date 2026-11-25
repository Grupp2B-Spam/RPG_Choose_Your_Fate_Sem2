package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MOVED: {@code quest} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>A quest cannot exist without the scene offering it, which is what makes it content.
 * Tracking a quest per character stays on this side.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code
 * /admin/v1/scenes/{sceneId}/quests}.
 */
public interface QuestRepository extends JpaRepository<Quest, Integer> {
    List<Quest> findByScene_Id(Integer sceneId);
}
