package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * MIGRATED to StoryContentManager: {@code scene} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is the aggregate root StoryContentManager publishes as
 * {@code scene-content.updated}, carrying the whole scene — its choices, the NPCs
 * standing in it and the quests it offers — rather than a notification to go and
 * fetch it.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
public interface SceneRepository extends JpaRepository<Scene, Integer> {
    // Loads scenes with their choices and each choice's destination scene.
    @EntityGraph(attributePaths = {"chapter", "choices", "choices.destinationScene", "choices.destinationScene.chapter"})
    @Query("select distinct s from Scene s")
    List<Scene> findAllWithLookAhead();

    // Loads one scene with its choices and each choice's destination scene.
    @EntityGraph(attributePaths = {"chapter", "choices", "choices.destinationScene", "choices.destinationScene.chapter"})
    @Query("select s from Scene s where s.id = :id")
    Optional<Scene> findByIdWithLookAhead(@Param("id") Integer id);
}
