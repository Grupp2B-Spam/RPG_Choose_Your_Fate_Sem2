package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
