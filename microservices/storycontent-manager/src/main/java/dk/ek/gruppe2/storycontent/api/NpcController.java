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

/**
 * Authoring endpoints for NPCs.
 *
 * <p>Placing an NPC in a scene is not here — it is {@code PUT /admin/v1/scenes/{id}/npcs},
 * because the scene is the aggregate that gets published and the placement is part of its
 * payload. An NPC's own name is not published at all, so editing it publishes nothing.
 */
@RestController
@RequestMapping("/admin/v1/npcs")
public class NpcController {

    private final NpcService npcs;

    public NpcController(NpcService npcs) {
        this.npcs = npcs;
    }

    @GetMapping
    public List<NpcResponse> findAll() {
        return npcs.findAll();
    }

    @GetMapping("/{id}")
    public NpcResponse findById(@PathVariable Integer id) {
        return npcs.findById(id);
    }

    @PostMapping
    public ResponseEntity<NpcResponse> create(@Valid @RequestBody NpcRequest request) {
        NpcResponse created = npcs.create(request);
        return ResponseEntity.created(URI.create("/admin/v1/npcs/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public NpcResponse update(@PathVariable Integer id, @Valid @RequestBody NpcRequest request) {
        return npcs.update(id, request);
    }

    /**
     * Deletes an NPC, and republishes every scene it stood in.
     *
     * <p>A real delete, unlike scenes and items: nothing a player holds refers to an NPC,
     * so there is no recorded history to break.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        npcs.delete(id);
        return ResponseEntity.noContent().build();
    }
}
