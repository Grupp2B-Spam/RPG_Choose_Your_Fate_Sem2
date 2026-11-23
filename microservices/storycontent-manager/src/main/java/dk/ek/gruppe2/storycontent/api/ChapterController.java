package dk.ek.gruppe2.storycontent.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterResponse;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.StartingSceneRequest;
import dk.ek.gruppe2.storycontent.service.ChapterService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/v1/chapters")
public class ChapterController {

    private final ChapterService chapters;

    public ChapterController(ChapterService chapters) {
        this.chapters = chapters;
    }

    @GetMapping
    public List<ChapterResponse> findAll() {
        return chapters.findAll();
    }

    @GetMapping("/{id}")
    public ChapterResponse findById(@PathVariable Integer id) {
        return chapters.findById(id);
    }

    @PostMapping
    public ResponseEntity<ChapterResponse> create(@Valid @RequestBody ChapterRequest request) {
        ChapterResponse created = chapters.create(request);
        return ResponseEntity.created(URI.create("/admin/v1/chapters/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ChapterResponse update(@PathVariable Integer id, @Valid @RequestBody ChapterRequest request) {
        return chapters.update(id, request);
    }

    /**
     * Sets the scene this chapter begins at.
     *
     * <p>Its own endpoint rather than a field on the chapter, because it is not an
     * ordinary edit: this scene id is denormalised into every race that starts here, so
     * changing it republishes those races with new versions (ADR-017). Naming the
     * operation is what keeps that from being a surprising side effect of a rename.
     */
    @PutMapping("/{id}/starting-scene")
    public ChapterResponse setStartingScene(
            @PathVariable Integer id,
            @Valid @RequestBody StartingSceneRequest request) {
        return chapters.setStartingScene(id, request.sceneId());
    }
}
