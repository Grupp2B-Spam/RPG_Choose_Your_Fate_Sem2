package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemResponseDTO {

    private Integer id;
    private String name;
    private String description;
    private ItemType type;

    public ItemResponseDTO(Item item) {
        this.id = item.getId();
        this.name = item.getName();
        this.description = item.getDescription();
        this.type = item.getType();
    }
}
