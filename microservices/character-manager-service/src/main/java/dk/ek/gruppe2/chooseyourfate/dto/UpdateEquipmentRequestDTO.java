package dk.ek.gruppe2.chooseyourfate.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEquipmentRequestDTO {

    private Integer itemId;
    private String type;


}
