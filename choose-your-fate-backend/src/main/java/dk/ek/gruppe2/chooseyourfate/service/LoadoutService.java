package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.*;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Equipment;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Inventory;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
public class LoadoutService {

    private final EquipmentService equipmentService;
    private final ItemService itemService;
    private final InventoryService inventoryService;

    public LoadoutService(EquipmentService equipmentService, ItemService itemService, InventoryService inventoryService) {
        this.equipmentService = equipmentService;
        this.itemService = itemService;
        this.inventoryService = inventoryService;
    }


    public LoadoutResponseDTO getLoadoutByCharacterId(Integer characterId) {
        EquipmentResponseDTO equipment = equipmentService.getEquipmentByCharacterId(characterId);
        InventoryResponseDTO inventory = inventoryService.getInventoryByCharacterId(characterId);
        return toDto(equipment, inventory);
    }

    @Transactional
    public LoadoutResponseDTO unequipItem(Integer characterId, Integer itemId) {
        Item item = itemService.getItemEntity(itemId);
        Inventory inventory = inventoryService.getInventoryEntityByCharacterId(characterId);
        equipmentService.validateItemEquipped(characterId, item);
        Equipment equipment = equipmentService.getEquipmentEntity(characterId);
        switch (item.getType()) {
            case ARMOR_HEAD ->  {
                equipment.setHead(null);
            }
            case ARMOR_CHEST -> {
                equipment.setChest(null);
            }
            case ARMOR_LEGS -> {
                equipment.setLegs(null);
            }
            default -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item type is not supported for equipment");
            }
        }
        EquipmentResponseDTO updatedEquipment = equipmentService.updateEquipment(equipment);
        InventoryResponseDTO updatedInventory = inventoryService.addItemToInventory(inventory.getId(), itemId);
        return toDto(updatedEquipment, updatedInventory);
    }

    @Transactional
    public LoadoutResponseDTO equipItem(Integer characterId, Integer itemId) {
        Item item = itemService.getItemEntity(itemId);
        Inventory inventory = inventoryService.getInventoryEntityByCharacterId(characterId);
        Equipment equipment = equipmentService.getEquipmentEntity(characterId);
        InventoryResponseDTO updatedInventory;
        Item currentlyEquippedItem = null;
        inventoryService.validateItemInInventory(inventory.getId(), itemId);
        switch (item.getType()) {
            case ARMOR_HEAD ->  {
                if (equipment.getHead() != null) {
                    currentlyEquippedItem = equipment.getHead();
                }
                equipment.setHead(item);
            }
            case ARMOR_CHEST -> {
                if (equipment.getChest() != null) {
                    currentlyEquippedItem = equipment.getChest();
                }
                equipment.setChest(item);
            }
            case ARMOR_LEGS -> {
                if (equipment.getLegs() != null) {
                    currentlyEquippedItem = equipment.getLegs();
                }
                equipment.setLegs(item);
            }
            default -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item type is not supported for equipment");
            }
        }
        EquipmentResponseDTO updatedEquipment = equipmentService.updateEquipment(equipment);
        inventoryService.removeItem(inventory.getId(), itemId);
        if (currentlyEquippedItem != null && currentlyEquippedItem.getId() != null) {
            updatedInventory = inventoryService.addItemToInventory(inventory.getId(), currentlyEquippedItem.getId());
        }   else {
            updatedInventory = inventoryService.getInventoryByCharacterId(characterId);
        }
        return toDto(updatedEquipment, updatedInventory);
    }

    public LoadoutResponseDTO toDto(EquipmentResponseDTO equipment, InventoryResponseDTO inventory) {
        ArrayList<ItemResponseDTO> equippedItems = new ArrayList<>();
        equippedItems.add(equipment.headItem());
        equippedItems.add(equipment.chestItem());
        equippedItems.add(equipment.legsItem());
        return new LoadoutResponseDTO(inventory.getInventoryId(), equippedItems, inventory.getItems());
    }
}