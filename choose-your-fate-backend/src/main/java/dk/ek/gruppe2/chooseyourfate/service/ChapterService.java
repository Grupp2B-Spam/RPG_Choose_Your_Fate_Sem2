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

/**
 * MIGRATED to StoryContentManager: {@code chapter} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is one of the ten tables the content/state split assigned to that service. A
 * chapter is not published as an event of its own: it reaches consumers as
 * {@code chapterId} inside a scene payload and as {@code startingChapterId} inside a
 * race payload.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/chapters} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
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