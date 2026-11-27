package dk.ek.gruppe2.storycontent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.ek.gruppe2.storycontent.domain.Race;

public interface RaceRepository extends JpaRepository<Race, Integer> {

    boolean existsByNameIgnoreCase(String name);

    /**
     * The races affected when a chapter's starting scene changes. Their message carries that
     * scene id, so they need republishing even though nothing about them was edited.
     */
    List<Race> findAllByStartingChapterId(Integer chapterId);

    boolean existsByStartingChapterId(Integer chapterId);

    /** See SceneRepository.incrementVersion for why this is a statement, not a setter. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Race r set r.version = r.version + 1 where r.id = :id")
    int incrementVersion(@Param("id") Integer id);
}
