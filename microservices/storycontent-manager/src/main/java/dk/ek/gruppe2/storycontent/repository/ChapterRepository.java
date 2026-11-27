package dk.ek.gruppe2.storycontent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ek.gruppe2.storycontent.domain.Chapter;

/** A chapter has no version column: it is never published on its own. */
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
}
