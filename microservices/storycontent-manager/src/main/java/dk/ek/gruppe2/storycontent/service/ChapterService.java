package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.InvalidContentException;
import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterResponse;
import dk.ek.gruppe2.storycontent.domain.Chapter;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.repository.ChapterRepository;
import dk.ek.gruppe2.storycontent.repository.SceneRepository;

@Service
public class ChapterService {

    private final ChapterRepository chapters;
    private final SceneRepository scenes;
    private final RaceService raceService;

    public ChapterService(ChapterRepository chapters, SceneRepository scenes, RaceService raceService) {
        this.chapters = chapters;
        this.scenes = scenes;
        this.raceService = raceService;
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> findAll() {
        List<ChapterResponse> result = new ArrayList<>();
        for (Chapter chapter : chapters.findAll()) {
            result.add(toResponse(chapter));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ChapterResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public ChapterResponse create(ChapterRequest request) {
        Chapter chapter = new Chapter(request.name());
        chapters.save(chapter);
        return toResponse(chapter);
    }

    /** Renames a chapter. Publishes nothing: a chapter's name reaches no consumer. */
    @Transactional
    public ChapterResponse update(Integer id, ChapterRequest request) {
        Chapter chapter = require(id);
        chapter.setName(request.name());
        chapters.save(chapter);
        return toResponse(chapter);
    }

    /**
     * Sets the scene a chapter begins at. Unlike a rename, consumers can see this: a race
     * message carries this scene id, so every race starting here is republished.
     */
    @Transactional
    public ChapterResponse setStartingScene(Integer chapterId, Integer sceneId) {
        Chapter chapter = require(chapterId);
        Scene scene = scenes.findById(sceneId)
                .orElseThrow(() -> new NotFoundException("Scene " + sceneId + " does not exist"));

        if (!scene.getChapter().getId().equals(chapterId)) {
            throw new InvalidContentException(
                    "Scene " + sceneId + " belongs to chapter " + scene.getChapter().getId()
                            + " and cannot be the starting scene of chapter " + chapterId);
        }

        boolean changed = !Objects.equals(chapter.getStartingSceneId(), sceneId);
        chapter.setStartingSceneId(sceneId);
        chapters.save(chapter);

        if (changed) {
            raceService.republishRacesStartingIn(chapterId);
        }
        return toResponse(chapter);
    }

    private Chapter require(Integer id) {
        return chapters.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter " + id + " does not exist"));
    }

    private ChapterResponse toResponse(Chapter chapter) {
        return new ChapterResponse(chapter.getId(), chapter.getName(), chapter.getStartingSceneId());
    }
}
