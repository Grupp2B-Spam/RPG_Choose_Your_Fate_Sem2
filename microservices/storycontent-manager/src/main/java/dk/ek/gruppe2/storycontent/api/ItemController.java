package dk.ek.gruppe2.storycontent.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemResponse;
import dk.ek.gruppe2.storycontent.service.ItemService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/v1/items")
public class ItemController {

    private final ItemService items;

    public ItemController(ItemService items) {
        this.items = items;
    }

    @GetMapping
    public List<ItemResponse> findAll() {
        return items.findAll();
    }

    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable Integer id) {
        return items.findById(id);
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        ItemResponse created = items.create(request);
        return ResponseEntity.created(URI.create("/admin/v1/items/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Integer id, @Valid @RequestBody ItemRequest request) {
        return items.update(id, request);
    }

    /** Retires rather than deletes: a player who owns one must still be able to use it. */
    @DeleteMapping("/{id}")
    public ItemResponse retire(@PathVariable Integer id) {
        return items.retire(id);
    }
}
