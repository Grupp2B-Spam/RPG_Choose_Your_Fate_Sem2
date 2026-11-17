package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.EquipmentResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.EquipmentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {


    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentResponseDTO> getAllEquipment() {
        return equipmentService.getAllEquipment();
    }

    @GetMapping("/{characterId}")
    public EquipmentResponseDTO getEquipmentByCharacterId(@PathVariable Integer characterId
    ) {
        return equipmentService.getEquipmentByCharacterId(characterId);
    }

}
