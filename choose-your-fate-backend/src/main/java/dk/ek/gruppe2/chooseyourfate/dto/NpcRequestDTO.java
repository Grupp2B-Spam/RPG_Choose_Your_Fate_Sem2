package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NpcRequestDTO {

    private String name;
    private Integer raceDetailsId;

    public Npc getNpcEntity(RaceDetails raceDetails) {
        Npc npc = new Npc();
        npc.setName(this.name);
        npc.setRaceDetails(raceDetails);
        return npc;
    }
}
