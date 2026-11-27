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
     * Loads a scene with everything the published payload needs, instead of one query per
     * choice for its requirements and effects.
     *
     * Only one collection is fetched per query. Two bags in one query makes the database
     * return their cartesian product, which Hibernate rejects. The rest are loaded through
     * the batch fetch size in application.yml.
     */
    @Query("""
            select distinct s from Scene s
            left join fetch s.choices c
            where s.id = :id
            """)
    Optional<Scene> findByIdForPublishing(@Param("id") Integer id);

    /**
     * Increments the version in one statement rather than reading it into the entity and
     * writing it back. Two authors saving at once would otherwise read the same number and
     * write its successor, so two different payloads would carry the same version and a
     * consumer would discard one of them at random.
     *
     * @return the number of rows updated, which is 0 if the scene no longer exists
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Scene s set s.version = s.version + 1 where s.id = :id")
    int incrementVersion(@Param("id") Integer id);
}
