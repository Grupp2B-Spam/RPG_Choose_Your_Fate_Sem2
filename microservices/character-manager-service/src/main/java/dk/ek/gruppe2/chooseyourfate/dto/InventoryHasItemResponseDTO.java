package dk.ek.gruppe2.chooseyourfate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryHasItemResponseDTO {

    private Integer inventoryId;
    private Integer amount;
    private ItemResponseDTO item;

    public InventoryHasItemResponseDTO(Integer amount, ItemResponseDTO item) {
        this.amount = amount;
        this.item = item;
    }

    public InventoryHasItemResponseDTO(Integer inventoryId, Integer amount, ItemResponseDTO item) {
        this.inventoryId = inventoryId;
        this.amount = amount;
        this.item = item;
    }
}
