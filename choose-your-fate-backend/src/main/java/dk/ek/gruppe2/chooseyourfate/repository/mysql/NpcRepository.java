package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code npc} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>Only {@code npcIds} travels in the scene payload there, so an NPC's name is not
 * something any consumer receives.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/npcs} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
public interface NpcRepository extends JpaRepository<Npc, Integer> {
    List<Npc> findByRaceDetails_Id(Integer raceDetailsId);

    boolean existsByRaceDetails_Id(Integer raceDetailsId);
}
