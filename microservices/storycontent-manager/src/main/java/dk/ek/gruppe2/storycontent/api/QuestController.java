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

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestResponse;
import dk.ek.gruppe2.storycontent.service.QuestService;
import jakarta.validation.Valid;

/**
 * Authoring endpoints for quests.
 *
 * <p>A quest is created under the scene that offers it, because {@code scene_id} is
 * mandatory — a quest cannot exist without one. Reading and editing it afterwards is by
 * its own id, since the scene adds nothing to those operations.
 */
@RestController
@RequestMapping("/admin/v1")
public class QuestController {

    private final QuestService quests;

    public QuestController(QuestService quests) {
        this.quests = quests;
    }

    @GetMapping("/scenes/{sceneId}/quests")
    public List<QuestResponse> findAllInScene(@PathVariable Integer sceneId) {
        return quests.findAllInScene(sceneId);
    }

    @PostMapping("/scenes/{sceneId}/quests")
    public ResponseEntity<QuestResponse> create(
            @PathVariable Integer sceneId,
            @Valid @RequestBody QuestRequest request) {
        QuestResponse created = quests.create(sceneId, request);
        return ResponseEntity.created(URI.create("/admin/v1/quests/" + created.id())).body(created);
    }

    @GetMapping("/quests/{id}")
    public QuestResponse findById(@PathVariable Integer id) {
        return quests.findById(id);
    }

    @PutMapping("/quests/{id}")
    public QuestResponse update(@PathVariable Integer id, @Valid @RequestBody QuestRequest request) {
        return quests.update(id, request);
    }

    @DeleteMapping("/quests/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        quests.delete(id);
        return ResponseEntity.noContent().build();
    }
}
