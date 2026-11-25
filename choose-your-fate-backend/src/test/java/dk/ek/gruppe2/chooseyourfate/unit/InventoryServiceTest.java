package dk.ek.gruppe2.chooseyourfate.unit;

import dk.ek.gruppe2.chooseyourfate.dto.InventoryHasItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.InventoryResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.enums.ItemType;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.*;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryHasItemRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.InventoryRepository;
import dk.ek.gruppe2.chooseyourfate.service.InventoryService;
import dk.ek.gruppe2.chooseyourfate.service.ItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    InventoryHasItemRepository inventoryHasItemRepository;

    @Mock
    InventoryRepository inventoryRepository;

    @Mock
    ItemService itemService;

    @InjectMocks
    InventoryService inventoryService;

    @Test
    void getInventoryByCharacterId_ShouldReturnDTO_WhenInventoryExists() {
        // Arrange
        CharacterAvatar character = new CharacterAvatar();
        character.setName("TestCharacter");

        Inventory fakeInventory = new Inventory();
        fakeInventory.setId(1);
        fakeInventory.setCharacter(character);

        when(inventoryRepository.findByCharacter_Id(1)).thenReturn(fakeInventory);
        when(inventoryHasItemRepository.findByInventory_Id(1)).thenReturn(List.of());

        // Act
        InventoryResponseDTO result = inventoryService.getInventoryByCharacterId(1);

        // Assert
        assertNotNull(result);
    }

    @Test
    void getInventoryByCharacterId_ShouldThrow_WhenInventoryNotFound() {
        // Arrange
        when(inventoryRepository.findByCharacter_Id(1)).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                inventoryService.getInventoryByCharacterId(1)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 5})
    void removeItem_ShouldReduceAmountBy1_WhenAmountGreaterThanOne(int amount) {

        //Assert
        Inventory fakeInventory = new Inventory();
        Item fakeItem = new Item();

        InventoryHasItem fakeInventoryHasItem = new InventoryHasItem();

        fakeInventoryHasItem.setInventory(fakeInventory);
        fakeInventoryHasItem.setItem(fakeItem);
        fakeInventory.setId(1);
        fakeItem.setId(1);
        fakeInventoryHasItem.setAmount(amount);
        when(inventoryHasItemRepository.findById(new InventoryHasItemId(fakeInventory.getId(), fakeItem.getId()))).thenReturn(Optional.of(fakeInventoryHasItem));

        //Act
        inventoryService.removeItem(1, 1);

        assertEquals(amount -1, fakeInventoryHasItem.getAmount());
        verify(inventoryHasItemRepository).save(fakeInventoryHasItem);

    }

    @Test
    void removeItem_ShouldRemoveItem_WhenAmountIsOne() {
        //Assert
        Inventory fakeInventory = new Inventory();
        Item fakeItem = new Item();

        InventoryHasItem fakeInventoryHasItem = new InventoryHasItem();

        fakeInventoryHasItem.setInventory(fakeInventory);
        fakeInventoryHasItem.setItem(fakeItem);
        fakeInventory.setId(1);
        fakeItem.setId(1);
        fakeInventoryHasItem.setAmount(1);
        when(inventoryHasItemRepository.findById(new InventoryHasItemId(fakeInventory.getId(), fakeItem.getId()))).thenReturn(Optional.of(fakeInventoryHasItem));

        //Act
        inventoryService.removeItem(1, 1);

        verify(inventoryHasItemRepository).delete(fakeInventoryHasItem);
    }

    @Test
    void removeItem_ShouldThrow_WhenAmountLessThanOne() {

        InventoryHasItem fakeInventoryHasItem =
                new InventoryHasItem();

        fakeInventoryHasItem.setAmount(0);

        when(inventoryHasItemRepository.findById(
                new InventoryHasItemId(1, 1)))
                .thenReturn(Optional.of(fakeInventoryHasItem));

        assertThrows(
                ResourceNotFoundException.class,
                () -> inventoryService.removeItem(1, 1)
        );
    }

    @Test
    void removeItem_ShouldThrow_WhenItemNotInInventory() {
        // Arrange
        when(inventoryHasItemRepository.findById(
                new InventoryHasItemId(1, 1)))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.removeItem(1, 1));
    }

    @Test
    void addItemToInventory_ShouldCreateNewEntry_WhenItemNotAlreadyInInventory() {
        //Arrange
        CharacterAvatar fakeCharacter = new CharacterAvatar();
        fakeCharacter.setId(1);
        Inventory fakeInventory = new Inventory();
        fakeInventory.setId(1);
        Item fakeItem = new Item();
        fakeItem.setId(1);
        fakeInventory.setCharacter(fakeCharacter);

        when(inventoryRepository.findById(1)).thenReturn(Optional.of(fakeInventory));
        when(itemService.getItemEntity(1)).thenReturn(fakeItem);
        when(inventoryHasItemRepository.existsByInventoryIdAndItemId(1, 1)).thenReturn(false);
        when(inventoryHasItemRepository.findByInventory_Id(1)).thenReturn(List.of());

        //Act
        inventoryService.addItemToInventory(1, 1);

        //Assert
        verify(inventoryHasItemRepository).save(any(InventoryHasItem.class));
    }

    @Test
    void addItemToInventory_ShouldIncrementAmount_WhenItemAlreadyInInventory() {
        CharacterAvatar fakeCharacter = new CharacterAvatar();
        fakeCharacter.setId(1);
        Inventory fakeInventory = new Inventory();
        fakeInventory.setId(1);
        Item fakeItem = new Item();
        fakeItem.setId(1);
        fakeInventory.setCharacter(fakeCharacter);

        InventoryHasItem existing = new InventoryHasItem();
        existing.setInventory(fakeInventory);
        existing.setItem(fakeItem);
        existing.setAmount(2);

        when(inventoryRepository.findById(1)).thenReturn(Optional.of(fakeInventory));
        when(itemService.getItemEntity(1)).thenReturn(fakeItem);
        when(inventoryHasItemRepository.existsByInventoryIdAndItemId(1, 1)).thenReturn(true);
        when(inventoryHasItemRepository.findById(new InventoryHasItemId(1, 1))).thenReturn(Optional.of(existing));
        when(inventoryHasItemRepository.findByInventory_Id(1)).thenReturn(List.of());

        inventoryService.addItemToInventory(1, 1);

        assertEquals(3, existing.getAmount());
        verify(inventoryHasItemRepository).save(existing);
    }

    @Test
    void addItemToInventory_ShouldThrow_WhenInventoryDoesNotExist() {
        when(inventoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () ->
                inventoryService.addItemToInventory(1, 1)
        );
    }
    
    @Test
    void toInventoryHasItemDTO_ShouldReturnDTO_WhenValidItemInInventory() {
        // Arrange
        Item fakeItem = new Item();
        fakeItem.setId(1);

        InventoryHasItem fakeInventoryHasItem = new InventoryHasItem();
        fakeInventoryHasItem.setItem(fakeItem);
        fakeInventoryHasItem.setAmount(3);

        ItemResponseDTO fakeItemResponseDTO = new ItemResponseDTO(1,"TestItem", "Sword of Testing", ItemType.WEAPON);

        when(itemService.toDto(fakeItem)).thenReturn(fakeItemResponseDTO);

        // Act
        InventoryHasItemResponseDTO result = inventoryService.toInventoryHasItemDTO(fakeInventoryHasItem);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getAmount());
        assertEquals(fakeItemResponseDTO, result.getItem());
    }

    @Test
    void toInventoryHasItemDTO_ShouldThrow_WhenNullIsPassed() {
        assertThrows(NullPointerException.class, () ->
                inventoryService.toInventoryHasItemDTO(null)
        );
    }

    @Test
    void toInventoryResponseDTO_ShouldReturnDTO_WhenValidInventory() {
        // Arrange
        CharacterAvatar character = new CharacterAvatar();
        character.setName("TestCharacter");

        Inventory fakeInventory = new Inventory();
        fakeInventory.setId(1);
        fakeInventory.setCharacter(character);

        when(inventoryHasItemRepository.findByInventory_Id(1)).thenReturn(List.of());

        // Act
        InventoryResponseDTO result = inventoryService.toInventoryResponseDTO(fakeInventory);

        // Assert
        assertNotNull(result);
        assertEquals("TestCharacter", result.getCharacterName());
    }

    @Test
    void toInventoryResponseDTO_ShouldThrow_WhenNullIsPassed() {
        assertThrows(NullPointerException.class, () ->
                inventoryService.toInventoryResponseDTO(null)
        );
    }

    @Test
    void validateItemInInventory_ShouldNotThrow_WhenItemExists() {

        // Arrange
        when(inventoryHasItemRepository
                .existsByInventoryIdAndItemId(1, 1))
                .thenReturn(true);

        // Act + Assert
        assertDoesNotThrow(() ->
                inventoryService.validateItemInInventory(1, 1));
    }

    @Test
    void validateItemInInventory_ShouldThrow_WhenItemDoesNotExist() {

        // Arrange
        when(inventoryHasItemRepository
                .existsByInventoryIdAndItemId(1, 1))
                .thenReturn(false);

        // Act + Assert
        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.validateItemInInventory(1, 1));
    }
}