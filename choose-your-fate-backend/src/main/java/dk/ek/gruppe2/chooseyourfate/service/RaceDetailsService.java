package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.RaceDetailsResponseDTO;
import dk.ek.gruppe2.chooseyourfate.exception.DuplicateResourceException;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;
import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChapterRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterAvatarRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.NpcRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.RaceDetailsRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code race_details} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is called {@code race} there, and is published as
 * {@code race-definition.updated}. It went to StoryContentManager rather than
 * CharacterManager because {@code starting_chapter_id} is a mandatory reference into
 * story content: the other way round, CharacterManager would hold a foreign key into
 * another service's database. CharacterManager is expected to cache the event
 * instead, and {@code character_avatar.race_detail_id} becomes a plain integer.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/races} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Service
public class RaceDetailsService {

    private final RaceDetailsRepository raceDetailsRepository;
    private final CharacterAvatarRepository characterAvatarRepository;
    private final NpcRepository npcRepository;
    private final ChapterRepository chapterRepository;

    public RaceDetailsService(
            RaceDetailsRepository raceDetailsRepository,
            CharacterAvatarRepository characterAvatarRepository,
            NpcRepository npcRepository,
            ChapterRepository chapterRepository
    ) {
        this.raceDetailsRepository = raceDetailsRepository;
        this.characterAvatarRepository = characterAvatarRepository;
        this.npcRepository = npcRepository;
        this.chapterRepository = chapterRepository;
    }

    public List<RaceDetailsResponseDTO> getAllRaceDetails() {
        return raceDetailsRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public RaceDetailsResponseDTO getRaceDetailsById(Integer id) {
        return toDto(getRaceDetailsEntity(id));
    }

    public RaceDetailsResponseDTO createRaceDetails(RaceDetailsRequestDTO request) {
        try {
            RaceDetails raceDetails = new RaceDetails();
            raceDetails.setName(request.getName());
            raceDetails.setStartingChapter(getChapterById(request.getStartingChapterId()));
            RaceDetails saved = raceDetailsRepository.save(raceDetails);
            return toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Unable to create race details due to a data integrity conflict.");
        }
    }

    public void deleteRaceDetails(Integer id) {
        if (!raceDetailsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Race details not found with id: " + id);
        }

        // Race details are a shared lookup entity, so delete must be blocked while characters or NPCs still reference it.
        if (characterAvatarRepository.existsByRaceDetails_Id(id) || npcRepository.existsByRaceDetails_Id(id)) {
            throw new IllegalArgumentException("Race details cannot be deleted while referenced by characters or NPCs.");
        }

        raceDetailsRepository.deleteById(id);
    }

    private RaceDetails getRaceDetailsEntity(Integer id) {
        return raceDetailsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Race details not found with id: " + id));
    }

    private Chapter getChapterById(Integer chapterId){
        return chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found with id: " + chapterId));
    }

    private RaceDetailsResponseDTO toDto(RaceDetails raceDetails) {
        return new RaceDetailsResponseDTO(raceDetails.getId(), raceDetails.getName(), raceDetails.getStartingChapter().getId());
    }
}
