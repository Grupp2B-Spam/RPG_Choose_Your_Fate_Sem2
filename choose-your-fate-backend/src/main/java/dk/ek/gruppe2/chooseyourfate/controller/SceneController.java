package dk.ek.gruppe2.chooseyourfate.controller;

import java.util.List;

import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneLookaheadResponseDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.chooseyourfate.dto.scene.CreateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.UpdateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.enums.DataSourceType;
import dk.ek.gruppe2.chooseyourfate.service.SceneService;


@RestController
@RequestMapping("/choose-your-fate/scene")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping
    public List<SceneResponseDTO> getAllscenes() {
        return sceneService.getAllScenes();
    }

    @GetMapping("/{id}")
    public SceneResponseDTO getsceneById(
            @PathVariable Integer id
    ) {
        return sceneService.getSceneById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SceneResponseDTO createscene(
            @RequestBody CreateSceneRequestDTO request
    ) {
        return sceneService.createScene(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SceneResponseDTO updatescene(
            @PathVariable Integer id,
            @RequestBody UpdateSceneRequestDTO request
    ) {
        return sceneService.updateScene(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletescene(
            @PathVariable Integer id
    ) {
        sceneService.deleteScene(id);
    }

    @GetMapping("/{id}/lookahead")
    public SceneLookaheadResponseDTO getSceneLookahead(
            @PathVariable Integer id
    ) {
        return sceneService.getSceneLookahead(id);
    }

}