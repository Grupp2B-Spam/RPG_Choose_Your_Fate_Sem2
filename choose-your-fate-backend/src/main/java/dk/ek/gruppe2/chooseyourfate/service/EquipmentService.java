package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.EquipmentResponseDTO;
import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Equipment;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.EquipmentRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryHasItemRepository;
import dk.ek.gruppe2.chooseyourfate.service.InventoryService;
import dk.ek.gruppe2.chooseyourfate.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EquipmentService{

    private final EquipmentRepository equipmentRepository;
    private final ItemService itemService;

    public EquipmentService(EquipmentRepository equipmentRepository, ItemService itemService, InventoryService inventoryService, InventoryHasItemRepository inventoryHasItemRepository) {
        this.equipmentRepository = equipmentRepository;
        this.itemService = itemService;
    }

    public List<EquipmentResponseDTO> getAllEquipment() {
        return equipmentRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public EquipmentResponseDTO getEquipmentByCharacterId(Integer characterId) {
        return toDto(getEquipmentEntity(characterId));
    }


    public EquipmentResponseDTO updateEquipment(Equipment equipment) {
        Equipment updatedEquipment = equipmentRepository.save(equipment);
        return toDto(updatedEquipment);
    }

    public Equipment getEquipmentEntity(Integer characterId) {
        return equipmentRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found for character id: " + characterId));
    }

    private Item resolveItem(Integer itemId) {
        if (itemId == null) {
            return null;
        }

        return itemService.getItemEntity(itemId);
    }

    private EquipmentResponseDTO toDto(Equipment equipment) {
        return new EquipmentResponseDTO(
                equipment.getCharacterId(),
                equipment.getHead() == null ? null : itemService.toDto(equipment.getHead()),
                equipment.getChest() == null ? null : itemService.toDto(equipment.getChest()),
                equipment.getLegs() == null ? null : itemService.toDto(equipment.getLegs())
        );
    }

    private ItemType resolveItemType(Equipment equipment, Integer itemId) {
        if(equipment.getHead().getId().equals(itemId)) {
            return equipment.getHead().getType();
        }
        if(equipment.getChest().getId().equals(itemId)) {
            return equipment.getChest().getType();
        }
        if(equipment.getLegs().getId().equals(itemId)) {
            return equipment.getLegs().getType();
        }
        else {
            return null;
        }
    }


    public void validateItemEquipped(Integer characterId, Item item) {
        Equipment equipment = equipmentRepository.findById(characterId).orElseThrow(() -> new ResourceNotFoundException("Equipment not found for character id: " + characterId));
        switch (item.getType()) {
            case ARMOR_HEAD -> {
                if (equipment.getHead() == null || !equipment.getHead().getId().equals(item.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Character does not have this head item equipped");
                }
            }
            case ARMOR_CHEST -> {
                if (equipment.getChest() == null || !equipment.getChest().getId().equals(item.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Character does not have this chest item equipped");
                }

            }
            case ARMOR_LEGS -> {
                if (equipment.getLegs() == null || !equipment.getLegs().getId().equals(item.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Character does not have this leg item equipped");
                }

            }
            default -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item type: " + item.getType());
            }
        }
    }
}