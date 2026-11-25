package dk.ek.gruppe2.chooseyourfate.integration;

import dk.ek.gruppe2.chooseyourfate.dto.InventoryResponseDTO;
import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.*;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterAvatarRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryHasItemRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ItemRepository;
import dk.ek.gruppe2.chooseyourfate.service.InventoryService;
import dk.ek.gruppe2.chooseyourfate.service.ItemService;
import dk.ek.gruppe2.chooseyourfate.TestContainerConfig;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
@Transactional
public class InventoryServiceIT {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.MYSQL::getJdbcUrl);
        registry.add("spring.datasource.password", TestContainerConfig.MYSQL::getPassword);
        registry.add("spring.datasource.username", TestContainerConfig.MYSQL::getUsername);
    }

    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    InventoryHasItemRepository inventoryHasItemRepository;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    CharacterAvatarRepository characterAvatarRepository;

    @Autowired
    ItemService itemService;

    @Autowired
    InventoryService inventoryService;

    //Arrange is largely done by the seed data script

    @Test
    void getInventoryByCharacterId_ShouldReturnDTO_WhenInventoryExists() {

        //Arrange
        Integer query = 1;

        //Act
        InventoryResponseDTO result = inventoryService.getInventoryByCharacterId(query);

        //Assert
        assertEquals("Lyra", result.getCharacterName(), "Character name should be Lyra");
        assertEquals(1, result.getInventoryId(), "Inventory id should be 1");
    }

    @Test
    void getInventoryByCharacterId_ShouldThrowException_WhenCharacterDoesNotExist() {
        //Arrange
        Integer characterId = 99999;
        //Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> inventoryService.getInventoryByCharacterId(characterId),
                "Expected ResourceNotFoundException for nonexistent character id"
        );

    }

    @ParameterizedTest
    @ValueSource(ints = {2, 10})
    void removeItem_ShouldDecrementAmount_WhenAmountGreaterThan1(int amount) {
        // Arrange
        Item item = new Item();
        item.setType(ItemType.WEAPON);
        item.setName("Sword of Testing");
        Item savedItem = itemRepository.save(item);

        Inventory inventory = inventoryRepository.findByCharacter_Id(1);
        InventoryHasItem inventoryHasItem = new InventoryHasItem(item, inventory, amount);

        inventoryHasItemRepository.save(inventoryHasItem);

        // Act
        inventoryService.removeItem(inventory.getId(), savedItem.getId());

        // Assert
        InventoryHasItem result = inventoryHasItemRepository
                .findById(new InventoryHasItemId(savedItem.getId(), inventory.getId())).get();
        assertEquals(amount - 1, result.getAmount());
    }

    @Test
    void removeItem_ShouldRemoveInventoryHasItemRow_WhenAmountIsEqualToOne() {

        // Arrange
        Item item = new Item();
        item.setType(ItemType.WEAPON);
        item.setName("Sword of Testing");
        Item savedItem = itemRepository.save(item);

        Inventory inventory = inventoryRepository.findByCharacter_Id(1);
        InventoryHasItem inventoryHasItem = new InventoryHasItem(savedItem, inventory, 1);

        inventoryHasItemRepository.save(inventoryHasItem);

        // Act
        inventoryService.removeItem(inventory.getId(), savedItem.getId());

        // Assert
        assertFalse(inventoryHasItemRepository
                        .existsById(new InventoryHasItemId(savedItem.getId(), inventory.getId())),
                "Row should have been deleted when amount reached 1"
        );
    }

    @Test
    void removeItem_ShouldThrow_WhenItemNotInInventory() {
        assertThrows(ResourceNotFoundException.class, () ->
                        inventoryService.removeItem(99999, 99999),
                "Expected ResourceNotFoundException when item not in inventory"
        );
    }

    @Test
    void addItemToInventory_ShouldCreateNewEntry_WhenItemNotInInventory() {
        // Arrange
        Item newItem = new Item();
        newItem.setName("TestItem");
        Item savedItem = itemRepository.save(newItem);

        // Act
        inventoryService.addItemToInventory(1, savedItem.getId());

        // Assert
        assertTrue(inventoryHasItemRepository
                        .existsByInventoryIdAndItemId(1, savedItem.getId()),
                "Expected new InventoryHasItem row to be created"
        );
    }

    @Test
    void addItemToInventory_ShouldIncreaseAmountByOne_WhenItemInInventory() {
        //Arrange
        Item newItem = new Item();
        newItem.setName("TestItem");
        newItem.setType(ItemType.CONSUMABLE);
        Item savedItem = itemRepository.save(newItem);
        Inventory inventory = inventoryRepository.findByCharacter_Id(1);
        inventoryHasItemRepository.save(new InventoryHasItem(savedItem, inventory, 1));

        //Act
        inventoryService.addItemToInventory(1, savedItem.getId());
        Optional<InventoryHasItem> result = inventoryHasItemRepository.findById(new InventoryHasItemId(savedItem.getId(), inventory.getId()));

        //Assert
        assertEquals(2, result.get().getAmount(), "Amount should be 2");
    }

}
