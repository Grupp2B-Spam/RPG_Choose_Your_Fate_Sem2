package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MOVED: {@code chapter} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>It has no event of its own; it reaches consumers inside scene and race messages.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/chapters}.
 */
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
}
