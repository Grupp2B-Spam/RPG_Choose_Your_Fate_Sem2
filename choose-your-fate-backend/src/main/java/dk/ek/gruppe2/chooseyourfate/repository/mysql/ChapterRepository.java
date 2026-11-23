package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MIGRATED to StoryContentManager: {@code chapter} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is one of the ten tables the content/state split assigned to that service. A
 * chapter is not published as an event of its own: it reaches consumers as
 * {@code chapterId} inside a scene payload and as {@code startingChapterId} inside a
 * race payload.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/chapters} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
}
