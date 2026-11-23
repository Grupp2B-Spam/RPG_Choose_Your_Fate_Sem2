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
 * <p>Under {@code /admin} because every route here writes content, and content is written
 * by the writing team rather than by players. The role check that enforces that is not
 * wired up yet — it waits on the JWT claim shape, which AccountManager owns
 * ({@code OPEN-DESIGN-ISSUES.md} 1.3). Said plainly here rather than left to be assumed:
 * these routes are currently open.
 *
 * <p>Versioned at {@code /v1} from the start. It costs nothing now and keeps the decision
 * open; adding a version to a published API later is the expensive direction.
 *
 * <p>Choices are sub-resources of a scene rather than a collection of their own. That is a
 * statement about the aggregate, not a URL preference: a choice cannot change without
 * changing the scene's published event, so there is no such thing as editing one on its
 * own.
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
