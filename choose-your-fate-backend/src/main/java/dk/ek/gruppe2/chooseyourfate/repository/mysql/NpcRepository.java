package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NpcRepository extends JpaRepository<Npc, Integer> {
    List<Npc> findByRaceDetails_Id(Integer raceDetailsId);

    boolean existsByRaceDetails_Id(Integer raceDetailsId);
}
