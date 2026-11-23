package dk.ek.gruppe2.storycontent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.ek.gruppe2.storycontent.domain.Npc;

public interface NpcRepository extends JpaRepository<Npc, Integer> {

    boolean existsByRaceId(Integer raceId);

    /**
     * The scenes an NPC stands in.
     *
     * <p>Needed when an NPC is deleted: every scene it was placed in loses an entry from
     * its {@code npcIds}, which is a change a consumer can observe, so each of those
     * scenes has to be republished with a new version.
     */
    @Query("select s.id from Scene s join s.npcs n where n.id = :npcId")
    List<Integer> findSceneIdsContaining(@Param("npcId") Integer npcId);
}
