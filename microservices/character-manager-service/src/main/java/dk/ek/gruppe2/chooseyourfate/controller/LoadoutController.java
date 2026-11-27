package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.LoadoutResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.LoadoutService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/choose-your-fate/loadout")
public class LoadoutController {

    private final LoadoutService loadoutService;

    public LoadoutController(LoadoutService loadoutService) {
        this.loadoutService = loadoutService;
    }

    @GetMapping("/{characterId}")
    public LoadoutResponseDTO getLoadout(@PathVariable Integer characterId)
    {
        return loadoutService.getLoadoutByCharacterId(characterId);
    }


    @PostMapping("/{characterId}/unequip")
    public LoadoutResponseDTO unequipItem(
            @PathVariable Integer characterId,
            @RequestBody Integer itemId
    ) {
        return loadoutService.unequipItem(characterId, itemId);
    }

    @PostMapping("/{characterId}/equip")
    public LoadoutResponseDTO equipItem(
            @PathVariable Integer characterId,
            @RequestBody Integer itemId
    ) {
        return loadoutService.equipItem(characterId, itemId);
    }



}
