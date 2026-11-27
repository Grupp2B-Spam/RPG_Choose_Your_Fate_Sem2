package dk.ek.gruppe2.storycontent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.ek.gruppe2.storycontent.domain.Npc;

public interface NpcRepository extends JpaRepository<Npc, Integer> {

    boolean existsByRaceId(Integer raceId);

    /**
     * The scenes an NPC stands in. Needed on delete: each one loses an npcIds entry, which a
     * consumer can see, so each has to be republished.
     */
    @Query("select s.id from Scene s join s.npcs n where n.id = :npcId")
    List<Integer> findSceneIdsContaining(@Param("npcId") Integer npcId);
}
