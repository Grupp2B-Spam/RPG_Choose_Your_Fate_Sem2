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

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceResponse;
import dk.ek.gruppe2.storycontent.service.RaceService;
import jakarta.validation.Valid;

/**
 * Authoring endpoints for races.
 *
 * <p>Note what is <em>not</em> here: a lookup for CharacterManager to call at character
 * creation. That is deliberate — it caches {@code race-definition.updated} instead, so
 * creating a character does not depend on this service being up. Adding a convenient
 * endpoint here would make the cache optional in practice, and the dependency would
 * quietly come back.
 */
@RestController
@RequestMapping("/admin/v1/races")
public class RaceController {

    private final RaceService races;

    public RaceController(RaceService races) {
        this.races = races;
    }

    @GetMapping
    public List<RaceResponse> findAll() {
        return races.findAll();
    }

    @GetMapping("/{id}")
    public RaceResponse findById(@PathVariable Integer id) {
        return races.findById(id);
    }

    @PostMapping
    public ResponseEntity<RaceResponse> create(@Valid @RequestBody RaceRequest request) {
        RaceResponse created = races.create(request);
        return ResponseEntity.created(URI.create("/admin/v1/races/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public RaceResponse update(@PathVariable Integer id, @Valid @RequestBody RaceRequest request) {
        return races.update(id, request);
    }

    /** Retires rather than deletes: existing characters have this race. */
    @DeleteMapping("/{id}")
    public RaceResponse retire(@PathVariable Integer id) {
        return races.retire(id);
    }
}
