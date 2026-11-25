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
 * Authoring endpoints for scenes.
 *
 * <p>Under {@code /admin} because every route writes content, which the writing team does
 * and players do not. <b>The role check is not wired up yet, so these routes are open</b>
 * — it waits on the JWT claim shape, which AccountManager owns and we have not agreed.
 *
 * <p>Versioned at {@code /v1} from the start: free now, expensive to add later.
 *
 * <p>Choices are a sub-resource of a scene, not a collection of their own. That is about
 * the aggregate rather than the URL: a choice cannot change without changing what the
 * scene publishes, so editing one on its own is not a thing.
 */
@RestController
@RequestMapping("/admin/v1/scenes")
public class SceneController {

    private final SceneService scenes;

    public SceneController(SceneService scenes) {
        this.scenes = scenes;
    }

    @GetMapping
    public List<SceneResponse> findAll() {
        return scenes.findAll();
    }

    @GetMapping("/{id}")
    public SceneResponse findById(@PathVariable Integer id) {
        return scenes.findById(id);
    }

    @PostMapping
    public ResponseEntity<SceneResponse> create(@Valid @RequestBody SceneRequest request) {
        SceneResponse created = scenes.create(request);
        return ResponseEntity.created(URI.create("/admin/v1/scenes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SceneResponse update(@PathVariable Integer id, @Valid @RequestBody SceneRequest request) {
        return scenes.update(id, request);
    }

    /**
     * Withdraws a scene from delivery.
     *
     * <p>{@code DELETE} in the URL, {@code RETIRED} in the data. Recorded paths point at
     * scenes a player has already been through, and after the decomposition no foreign key
     * stops an author from removing one — so the destructive version of this operation is
     * not offered at all rather than guarded.
     */
    @DeleteMapping("/{id}")
    public SceneResponse retire(@PathVariable Integer id) {
        return scenes.retire(id);
    }

    @PostMapping("/{id}/choices")
    public SceneResponse addChoice(@PathVariable Integer id, @Valid @RequestBody ChoiceRequest request) {
        return scenes.addChoice(id, request);
    }

    @PutMapping("/{id}/choices/{choiceId}")
    public SceneResponse replaceChoice(
            @PathVariable Integer id,
            @PathVariable Integer choiceId,
            @Valid @RequestBody ChoiceRequest request) {
        return scenes.replaceChoice(id, choiceId, request);
    }

    @DeleteMapping("/{id}/choices/{choiceId}")
    public SceneResponse deleteChoice(@PathVariable Integer id, @PathVariable Integer choiceId) {
        return scenes.deleteChoice(id, choiceId);
    }

    /** Replaces the whole set of NPCs standing in this scene. */
    @PutMapping("/{id}/npcs")
    public SceneResponse replaceNpcs(
            @PathVariable Integer id,
            @Valid @RequestBody SceneNpcsRequest request) {
        return scenes.replaceNpcs(id, request.npcIds());
    }
}
