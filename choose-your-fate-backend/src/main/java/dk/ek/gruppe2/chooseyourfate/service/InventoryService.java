package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.InventoryHasItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.InventoryResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Inventory;
import dk.ek.gruppe2.chooseyourfate.model.mysql.InventoryHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.InventoryHasItemId;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryHasItemRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class InventoryService {

    private final ItemService itemService;
    private final InventoryRepository inventoryRepository;
    private final InventoryHasItemRepository inventoryHasItemRepository;

    public InventoryService(InventoryRepository inventoryRepository, InventoryHasItemRepository inventoryHasItemRepository, ItemService itemService) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryHasItemRepository = inventoryHasItemRepository;
        this.itemService = itemService;
    }

    public InventoryResponseDTO getInventoryByCharacterId(Integer characterId) {
        Inventory inventory = inventoryRepository.findByCharacter_Id(characterId);
        if(inventory == null) {
            throw new ResourceNotFoundException("Inventory with character id " + characterId + " not found");
        }
        return toInventoryResponseDTO(inventory);
    }

    public Inventory getInventoryEntityByCharacterId(Integer characterId) {
        return inventoryRepository.findByCharacter_Id(characterId);
    }

    @Transactional
    public void removeItem(Integer inventoryId, Integer itemId) {
        InventoryHasItem existing = inventoryHasItemRepository
                .findById(new InventoryHasItemId(itemId, inventoryId))
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in inventory"));

        if (existing.getAmount() > 1) {
            existing.setAmount(existing.getAmount() - 1);
            inventoryHasItemRepository.save(existing);
        } else if (existing.getAmount() == 1) {
            inventoryHasItemRepository.delete(existing);
        } else {
            throw new ResourceNotFoundException("Item exists in inventory but amount is less than 1");
        }
    }

    public InventoryResponseDTO addItemToInventory(Integer inventoryId, Integer itemId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Item item = itemService.getItemEntity(itemId);
        if (inventoryHasItemRepository.existsByInventoryIdAndItemId(inventoryId, itemId)) {
            InventoryHasItem existing = inventoryHasItemRepository
                    .findById(new InventoryHasItemId(itemId, inventoryId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            existing.setAmount(existing.getAmount() + 1);
            inventoryHasItemRepository.save(existing);
        } else {
            inventoryHasItemRepository.save(new InventoryHasItem(item, inventory, 1));
        }
        return toInventoryResponseDTO(inventory);
    }

    public InventoryHasItemResponseDTO toInventoryHasItemDTO(InventoryHasItem itemInInventory) {
        ItemResponseDTO itemResponseDTO= itemService.toDto(itemInInventory.getItem());
        return new InventoryHasItemResponseDTO(itemInInventory.getAmount(), itemResponseDTO);
    }

    public InventoryResponseDTO toInventoryResponseDTO(Inventory inventory) {
        List<InventoryHasItem> inventoryHasItemList = inventoryHasItemRepository.findByInventory_Id(inventory.getId());
        List<InventoryHasItemResponseDTO> inventoryHasItemResponseDTOS = inventoryHasItemList.stream().map((this::toInventoryHasItemDTO)).toList();
        InventoryResponseDTO response = new InventoryResponseDTO(inventory.getId(), inventory.getCharacter().getName(), inventoryHasItemResponseDTOS);
        return response;
    }


    public void validateItemInInventory(Integer inventoryId, Integer itemId) {
        if (!inventoryHasItemRepository.existsByInventoryIdAndItemId(inventoryId, itemId)) {
            throw new ResourceNotFoundException("Item not found in inventory");
        }
    }
}
