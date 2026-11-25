package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.InventoryResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/choose-your-fate/inventories")
public class InventoryController {

    InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#id, authentication)")
    public InventoryResponseDTO getInventoryByCharacterId(@PathVariable Integer characterId) {
        return inventoryService.getInventoryByCharacterId(characterId);
    }

    @PostMapping("/{inventoryId}/items/{itemId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#id, authentication)")
    public InventoryResponseDTO addItemToInventory(@PathVariable Integer inventoryId, @PathVariable Integer itemId) {
        return inventoryService.addItemToInventory(inventoryId, itemId);
    }

    @PostMapping("/{inventoryId}/items/{itemId}/use")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#id, authentication)")
    public void removeItem(@PathVariable Integer inventoryId, @PathVariable Integer itemId) {
        inventoryService.removeItem(inventoryId, itemId);
    }
}
