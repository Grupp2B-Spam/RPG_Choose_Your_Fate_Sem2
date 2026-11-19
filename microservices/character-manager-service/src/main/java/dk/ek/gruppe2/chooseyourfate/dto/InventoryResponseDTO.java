package dk.ek.gruppe2.chooseyourfate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InventoryResponseDTO {

    private Integer inventoryId;
    private String characterName;
    private List<InventoryHasItemResponseDTO> items;

    public InventoryResponseDTO(Integer inventoryId, String characterName, List<InventoryHasItemResponseDTO> items) {
        this.inventoryId = inventoryId;
        this.characterName = characterName;
        this.items = items;
    }
}
