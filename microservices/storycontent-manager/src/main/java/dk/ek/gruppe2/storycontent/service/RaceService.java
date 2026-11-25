package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.InvalidContentException;
import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceResponse;
import dk.ek.gruppe2.storycontent.domain.Chapter;
import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import dk.ek.gruppe2.storycontent.domain.Race;
import dk.ek.gruppe2.storycontent.repository.ChapterRepository;
import dk.ek.gruppe2.storycontent.repository.RaceRepository;

/**
 * Authoring operations on race definitions.
 *
 * <p>Races show most clearly why the boundary sits where it does. Both content and player
 * state refer to a race, but a race itself <em>points into</em> content — its starting
 * chapter is mandatory — so it belongs here, and the reference from the player side
 * becomes a plain integer.
 */
@Service
public class RaceService {

    private static final Logger log = LoggerFactory.getLogger(RaceService.class);

    private final RaceRepository races;
    private final ChapterRepository chapters;
    private final ContentEventDispatcher dispatcher;

    public RaceService(RaceRepository races, ChapterRepository chapters, ContentEventDispatcher dispatcher) {
        this.races = races;
        this.chapters = chapters;
        this.dispatcher = dispatcher;
    }

    @Transactional(readOnly = true)
    public List<RaceResponse> findAll() {
        List<RaceResponse> result = new ArrayList<>();
        for (Race race : races.findAll()) {
            result.add(toResponse(race));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public RaceResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public RaceResponse create(RaceRequest request) {
        if (races.existsByNameIgnoreCase(request.name())) {
            throw new InvalidContentException("A race named '" + request.name() + "' already exists");
        }
        Race race = new Race(request.name(), requireChapter(request.startingChapterId()));
        races.save(race);

        dispatcher.raceChanged(race.getId());
        return toResponse(race);
    }

    @Transactional
    public RaceResponse update(Integer id, RaceRequest request) {
        Race race = require(id);
        if (!race.getName().equalsIgnoreCase(request.name()) && races.existsByNameIgnoreCase(request.name())) {
            throw new InvalidContentException("A race named '" + request.name() + "' already exists");
        }
        race.setName(request.name());
        race.setStartingChapter(requireChapter(request.startingChapterId()));
        races.save(race);
        return bumpAndPublish(id);
    }

    @Transactional
    public RaceResponse retire(Integer id) {
        Race race = require(id);
        race.setStatus(DefinitionStatus.RETIRED);
        races.save(race);
        return bumpAndPublish(id);
    }

    /**
     * Republishes every race that starts in a chapter whose starting scene has changed.
     *
     * <p>Called from {@code ChapterService}, not from the API. Nothing about the races was
     * edited — but a race message carries the chapter's starting scene, copied in so a new
     * character can be placed without a second lookup. Changing the chapter therefore
     * changes the right answer for every race beginning there, and a consumer caching them
     * would keep putting new characters in a scene the author moved away from.
     *
     * <p>This is the running cost of copying data into a message: obvious while writing
     * it, invisible six weeks later. Hence this comment and the test that covers it.
     */
    @Transactional
    public void republishRacesStartingIn(Integer chapterId) {
        List<Race> affected = races.findAllByStartingChapterId(chapterId);
        if (affected.isEmpty()) {
            return;
        }
        log.info("chapter {} changed its starting scene; republishing {} race definition(s) "
                + "that denormalise it", chapterId, affected.size());
        for (Race race : affected) {
            races.incrementVersion(race.getId());
            dispatcher.raceChanged(race.getId());
        }
    }

    private RaceResponse bumpAndPublish(Integer id) {
        races.incrementVersion(id);
        dispatcher.raceChanged(id);
        return toResponse(require(id));
    }

    private Race require(Integer id) {
        return races.findById(id)
                .orElseThrow(() -> new NotFoundException("Race " + id + " does not exist"));
    }

    private Chapter requireChapter(Integer id) {
        return chapters.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter " + id + " does not exist"));
    }

    private RaceResponse toResponse(Race race) {
        return new RaceResponse(
                race.getId(),
                race.getName(),
                race.getStartingChapter().getId(),
                race.getStartingChapter().getStartingSceneId(),
                race.getStatus(),
                race.getVersion());
    }
}
