package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * MOVED: {@code race_details} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Called {@code race} there and published as {@code race-definition.updated}. It went
 * with the content because its starting chapter is a mandatory reference into story
 * content.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/races}.
 */
public interface RaceDetailsRepository extends JpaRepository<RaceDetails, Integer> {
}