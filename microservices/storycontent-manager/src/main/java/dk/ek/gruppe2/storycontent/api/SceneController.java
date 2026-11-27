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

import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.ChoiceRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneNpcsRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneResponse;
import dk.ek.gruppe2.storycontent.service.SceneService;
import jakarta.validation.Valid;

/**
 * Authoring endpoints for scenes. Send X-API-VERSION: 1.
 *
 * No authorization yet: these routes are open to anyone who can reach the port. It waits
 * for AccountManager.
 */
@RestController
@RequestMapping("/admin/scenes")
public class SceneController {

    private final SceneService scenes;

    public SceneController(SceneService scenes) {
        this.scenes = scenes;
    }

    @GetMapping(version = "1")
    public List<SceneResponse> findAll() {
        return scenes.findAll();
    }

    @GetMapping(path = "/{id}", version = "1")
    public SceneResponse findById(@PathVariable Integer id) {
        return scenes.findById(id);
    }

    @PostMapping(version = "1")
    public ResponseEntity<SceneResponse> create(@Valid @RequestBody SceneRequest request) {
        SceneResponse created = scenes.create(request);
        return ResponseEntity.created(URI.create("/admin/scenes/" + created.id())).body(created);
    }

    @PutMapping(path = "/{id}", version = "1")
    public SceneResponse update(@PathVariable Integer id, @Valid @RequestBody SceneRequest request) {
        return scenes.update(id, request);
    }

    /** Retires the scene. Recorded player paths still point at it, so it is never deleted. */
    @DeleteMapping(path = "/{id}", version = "1")
    public SceneResponse retire(@PathVariable Integer id) {
        return scenes.retire(id);
    }

    @PostMapping(path = "/{id}/choices", version = "1")
    public SceneResponse addChoice(@PathVariable Integer id, @Valid @RequestBody ChoiceRequest request) {
        return scenes.addChoice(id, request);
    }

    @PutMapping(path = "/{id}/choices/{choiceId}", version = "1")
    public SceneResponse replaceChoice(
            @PathVariable Integer id,
            @PathVariable Integer choiceId,
            @Valid @RequestBody ChoiceRequest request) {
        return scenes.replaceChoice(id, choiceId, request);
    }

    @DeleteMapping(path = "/{id}/choices/{choiceId}", version = "1")
    public SceneResponse deleteChoice(@PathVariable Integer id, @PathVariable Integer choiceId) {
        return scenes.deleteChoice(id, choiceId);
    }

    /** Replaces the whole set of NPCs standing in this scene. */
    @PutMapping(path = "/{id}/npcs", version = "1")
    public SceneResponse replaceNpcs(
            @PathVariable Integer id,
            @Valid @RequestBody SceneNpcsRequest request) {
        return scenes.replaceNpcs(id, request.npcIds());
    }
}
