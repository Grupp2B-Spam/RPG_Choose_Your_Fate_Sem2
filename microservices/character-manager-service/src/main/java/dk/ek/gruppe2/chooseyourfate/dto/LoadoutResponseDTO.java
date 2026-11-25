package dk.ek.gruppe2.chooseyourfate.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoadoutResponseDTO {

    private Integer inventoryId;
    private List<ItemResponseDTO> equippedItems;
    private List<InventoryHasItemResponseDTO> itemsInInventory;

}
