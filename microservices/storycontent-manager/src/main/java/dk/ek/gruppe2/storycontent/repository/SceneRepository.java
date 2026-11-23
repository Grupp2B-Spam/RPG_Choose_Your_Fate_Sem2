package dk.ek.gruppe2.storycontent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.ek.gruppe2.storycontent.domain.Scene;

public interface SceneRepository extends JpaRepository<Scene, Integer> {

    List<Scene> findAllByChapterId(Integer chapterId);

    boolean existsByChapterId(Integer chapterId);

    /**
     * Loads a scene with everything the published payload needs.
     *
     * <p>Without this the payload builder triggers one query per choice for its
     * requirements and effects, and another per scene for NPCs and quests. That is the
     * N+1 problem, and it is worth avoiding here specifically because a scene is
     * republished on every edit — the cost is paid on the write path, which is also where
     * the broker is waiting.
     *
     * <p>Only one collection is fetched per query. Fetching two bags in one query makes
     * the database return the cartesian product of them, which Hibernate rejects outright.
     * The remaining collections are loaded by a second and third query through the batch
     * settings in {@code application.yml}, which is two round trips rather than N.
     */
    @Query("""
            select distinct s from Scene s
            left join fetch s.choices c
            where s.id = :id
            """)
    Optional<Scene> findByIdForPublishing(@Param("id") Integer id);

    /**
     * Increments the version in a single statement rather than by reading it into the
     * entity and writing it back.
     *
     * <p>This is what makes {@code sceneVersion} monotonic. Two authors saving the same
     * scene at once would both read the same number and both write its successor, so a
     * read-modify-write would hand two different payloads the same version — and a
     * consumer whose whole defence against stale messages is "discard anything not newer"
     * would then discard one of them at random. {@code version = version + 1} is resolved
     * by the database and cannot produce a duplicate.
     *
     * @return the number of rows updated, which is 0 if the scene no longer exists
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Scene s set s.version = s.version + 1 where s.id = :id")
    int incrementVersion(@Param("id") Integer id);
}
