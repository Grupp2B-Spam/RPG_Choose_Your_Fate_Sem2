package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MOVED: {@code choice} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Published as part of its scene, never on its own. Over there a choice also has a
 * {@code beat} and typed effects, which have no counterpart here.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/scenes/{id}/choices}.
 */
public interface ChoiceRepository extends JpaRepository<Choice, Integer> {
    List<Choice> findByScene_Id(Integer sceneId);
    List<Choice> findByDestinationScene_Id(Integer destinationSceneId);
}
