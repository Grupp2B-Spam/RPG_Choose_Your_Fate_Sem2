package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NpcResponseDTO {

    private String name;

    //private RaceDetailsResponseDTO raceDetailsResponseDTO;

    public NpcResponseDTO(Npc npc) {
        this.name = npc.getName();
    }
}
