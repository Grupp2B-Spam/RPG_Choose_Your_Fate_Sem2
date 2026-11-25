package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneLookaheadResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneResponseDTO;

import java.util.List;

import org.springframework.stereotype.Service;

import dk.ek.gruppe2.chooseyourfate.dto.scene.CreateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.UpdateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChapterRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;

@Service
public class SceneService {
    private final SceneRepository sceneRepository;
    private final ChapterRepository chapterRepository;

    public SceneService(SceneRepository sceneRepository, ChapterRepository chapterRepository) {
        this.sceneRepository = sceneRepository;
        this.chapterRepository = chapterRepository;
    }

    // Returns all SQL scenes with one-scene lookahead choices included.
    public List<SceneResponseDTO> getAllScenes() {
        return sceneRepository.findAllWithLookAhead()
                .stream()
                .map(SceneResponseDTO::new)
                .toList();
    }

    // Returns one SQL scene with the choices and destination scenes already loaded.
    public SceneResponseDTO getSceneById(Integer id) {
        return new SceneResponseDTO(getSceneEntity(id));
    }

    public SceneResponseDTO createScene(CreateSceneRequestDTO request) {
        Scene scene = request.toEntity(getChapterById(request.getChapterId()));
        return new SceneResponseDTO(sceneRepository.save(scene));
    }

    public SceneResponseDTO updateScene(Integer id, UpdateSceneRequestDTO request) {
        Scene scene = getSceneEntity(id);
        scene.setName(request.getName());
        scene.setChapter(getChapterById(request.getChapterId()));
        return new SceneResponseDTO(sceneRepository.save(scene));
    }

    public void deleteScene(Integer id) {
        if (!sceneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Scene not found with id: " + id);
        }
        sceneRepository.deleteById(id);
    }

    private Scene getSceneEntity(Integer id) {
        return sceneRepository.findByIdWithLookAhead(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + id));
    }

    private Chapter getChapterById(Integer chapterId){
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + chapterId));
    }

    public SceneLookaheadResponseDTO getSceneLookahead(Integer id) {
        return new SceneLookaheadResponseDTO(getSceneEntity(id));
    }
}