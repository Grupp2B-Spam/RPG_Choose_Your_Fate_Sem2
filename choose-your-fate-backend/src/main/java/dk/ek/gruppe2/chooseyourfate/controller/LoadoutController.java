package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.LoadoutResponseDTO;
import dk.ek.gruppe2.chooseyourfate.enums.DataSourceType;
import dk.ek.gruppe2.chooseyourfate.service.LoadoutService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/choose-your-fate/loadout")
public class LoadoutController {

    private final LoadoutService loadoutService;

    public LoadoutController(LoadoutService loadoutService) {
        this.loadoutService = loadoutService;
    }

    @GetMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public LoadoutResponseDTO getLoadout(@PathVariable Integer characterId)
    {
        return loadoutService.getLoadoutByCharacterId(characterId);
    }


    @PostMapping("/{characterId}/unequip")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public LoadoutResponseDTO unequipItem(
            @PathVariable Integer characterId,
            @RequestBody Integer itemId
    ) {
        return loadoutService.unequipItem(characterId, itemId);
    }

    @PostMapping("/{characterId}/equip")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public LoadoutResponseDTO equipItem(
            @PathVariable Integer characterId,
            @RequestBody Integer itemId
    ) {
        return loadoutService.equipItem(characterId, itemId);
    }



}
