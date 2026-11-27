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
 * Authoring endpoints for quests. A quest is created under the scene that offers it,
 * because scene_id is mandatory. It is read and edited afterwards by its own id.
 */
@RestController
@RequestMapping("/admin")
public class QuestController {

    private final QuestService quests;

    public QuestController(QuestService quests) {
        this.quests = quests;
    }

    @GetMapping(path = "/scenes/{sceneId}/quests", version = "1")
    public List<QuestResponse> findAllInScene(@PathVariable Integer sceneId) {
        return quests.findAllInScene(sceneId);
    }

    @PostMapping(path = "/scenes/{sceneId}/quests", version = "1")
    public ResponseEntity<QuestResponse> create(
            @PathVariable Integer sceneId,
            @Valid @RequestBody QuestRequest request) {
        QuestResponse created = quests.create(sceneId, request);
        return ResponseEntity.created(URI.create("/admin/quests/" + created.id())).body(created);
    }

    @GetMapping(path = "/quests/{id}", version = "1")
    public QuestResponse findById(@PathVariable Integer id) {
        return quests.findById(id);
    }

    @PutMapping(path = "/quests/{id}", version = "1")
    public QuestResponse update(@PathVariable Integer id, @Valid @RequestBody QuestRequest request) {
        return quests.update(id, request);
    }

    @DeleteMapping(path = "/quests/{id}", version = "1")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        quests.delete(id);
        return ResponseEntity.noContent().build();
    }
}
