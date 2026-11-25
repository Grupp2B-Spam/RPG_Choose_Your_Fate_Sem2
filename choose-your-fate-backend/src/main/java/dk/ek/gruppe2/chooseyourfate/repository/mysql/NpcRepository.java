package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MOVED: {@code npc} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Only NPC ids reach consumers, inside the scene message — a name is not published at
 * all.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/npcs}.
 */
public interface NpcRepository extends JpaRepository<Npc, Integer> {
    List<Npc> findByRaceDetails_Id(Integer raceDetailsId);

    boolean existsByRaceDetails_Id(Integer raceDetailsId);
}
