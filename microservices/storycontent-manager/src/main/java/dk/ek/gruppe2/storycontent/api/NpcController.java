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

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcResponse;
import dk.ek.gruppe2.storycontent.service.NpcService;
import jakarta.validation.Valid;

/** Authoring endpoints for NPCs. Placing one in a scene is PUT /admin/scenes/{id}/npcs. */
@RestController
@RequestMapping("/admin/npcs")
public class NpcController {

    private final NpcService npcs;

    public NpcController(NpcService npcs) {
        this.npcs = npcs;
    }

    @GetMapping(version = "1")
    public List<NpcResponse> findAll() {
        return npcs.findAll();
    }

    @GetMapping(path = "/{id}", version = "1")
    public NpcResponse findById(@PathVariable Integer id) {
        return npcs.findById(id);
    }

    @PostMapping(version = "1")
    public ResponseEntity<NpcResponse> create(@Valid @RequestBody NpcRequest request) {
        NpcResponse created = npcs.create(request);
        return ResponseEntity.created(URI.create("/admin/npcs/" + created.id())).body(created);
    }

    @PutMapping(path = "/{id}", version = "1")
    public NpcResponse update(@PathVariable Integer id, @Valid @RequestBody NpcRequest request) {
        return npcs.update(id, request);
    }

    /**
     * Deletes an NPC, and republishes every scene it stood in. A real delete, unlike scenes
     * and items: nothing a player holds refers to an NPC.
     */
    @DeleteMapping(path = "/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        npcs.delete(id);
        return ResponseEntity.noContent().build();
    }
}
