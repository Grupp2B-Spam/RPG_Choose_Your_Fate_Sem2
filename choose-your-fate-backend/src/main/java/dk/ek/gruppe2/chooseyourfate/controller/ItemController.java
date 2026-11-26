package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.ItemRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MOVED: {@code item} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Published as {@code item-definition.updated}. Its schema also splits what an item is
 * from where it is worn, which this {@code type} column mixes together.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/items}.
 */
@RestController
@RequestMapping("/choose-your-fate/items")
public class ItemController {

    ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemResponseDTO> getAllItems() {
        return itemService.getAllItems();
    }

    @GetMapping("/{id}")
    public ItemResponseDTO getItemById(@PathVariable("id") Integer id) {
        return itemService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponseDTO> createItem(@RequestBody ItemRequestDTO requestDTO) {
        return itemService.createItem(requestDTO);
    }

    @PutMapping("/{id}")
    public ItemResponseDTO updateItem(@PathVariable("id") Integer id, @RequestBody ItemRequestDTO requestDTO) {
        return itemService.updateItem(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteItem(@PathVariable("id") Integer id) {
        itemService.deleteItem(id);
    }
}

