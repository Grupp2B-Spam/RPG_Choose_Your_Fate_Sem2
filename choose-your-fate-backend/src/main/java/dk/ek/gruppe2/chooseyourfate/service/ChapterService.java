package dk.ek.gruppe2.chooseyourfate.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import dk.ek.gruppe2.chooseyourfate.dto.chapter.ChapterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.chapter.CreateChapterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.chapter.UpdateChapterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChapterRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;

//TODO: add handling for moving player to different chapter.
@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final SceneRepository sceneRepository;

    public ChapterService(ChapterRepository chapterRepository, SceneRepository sceneRepository) {
        this.chapterRepository = chapterRepository;
        this.sceneRepository = sceneRepository;
    }

    public List<ChapterResponseDTO> getAllChapters() {
        return chapterRepository.findAll()
                .stream()
                .map(ChapterResponseDTO::new)
                .toList();
    }

    public ChapterResponseDTO getChapterById(Integer id) {
        return new ChapterResponseDTO(getChapterEntity(id));
    }

    public ChapterResponseDTO createChapter(CreateChapterRequestDTO request) {
        Chapter chapter = request.toEntity();
        return new ChapterResponseDTO(chapterRepository.save(chapter));
    }

    public ChapterResponseDTO updateChapter(Integer id, UpdateChapterRequestDTO request) {
        Chapter chapter = getChapterEntity(id);
        chapter.setName(request.getName());
        chapter.setScenes(request.getScenes());
        chapter.setCharacters(request.getCharacters());
        if (request.getStartingSceneId() == null) {
            chapter.setStartingScene(null);
        } else {
            chapter.setStartingScene(sceneRepository.findById(request.getStartingSceneId())
                .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + request.getStartingSceneId())));
        }

        return new ChapterResponseDTO(chapterRepository.save(chapter));
    }

    public void deleteChapter(Integer id) {
        if (!chapterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Chapter not found with id: " + id);
        }
        chapterRepository.deleteById(id);
    }

    public Integer getStartingSceneByChapterId(Integer id) {
        return Optional.ofNullable(
            getChapterEntity(id).getStartingScene()
        )
        .map(Scene::getId)
        .orElseThrow(() ->
            new ResourceNotFoundException(
                "Scene not found for chapter id: " + id
            )
        );
    }

    private Chapter getChapterEntity(Integer id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + id));
    }
}