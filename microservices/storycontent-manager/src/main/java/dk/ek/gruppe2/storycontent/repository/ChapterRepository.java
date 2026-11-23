package dk.ek.gruppe2.storycontent.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.ek.gruppe2.storycontent.domain.Chapter;

/**
 * A chapter has no version column, because a chapter is never published on its own. It
 * reaches consumers only inside a scene payload ({@code chapterId}) and inside a race
 * payload ({@code startingChapterId}, {@code startingSceneId}), and those carry the
 * versions.
 */
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
}
