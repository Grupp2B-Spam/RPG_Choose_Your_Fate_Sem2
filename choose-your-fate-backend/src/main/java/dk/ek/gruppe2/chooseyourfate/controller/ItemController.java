package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.ItemRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code item} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>StoryContentManager publishes it as {@code item-definition.updated}. Its schema also
 * separates what an item is from where it is worn — this table's {@code type}
 * column conflates the two — and adds the requirements and modifiers the contract
 * publishes, which do not exist here.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/items} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
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

