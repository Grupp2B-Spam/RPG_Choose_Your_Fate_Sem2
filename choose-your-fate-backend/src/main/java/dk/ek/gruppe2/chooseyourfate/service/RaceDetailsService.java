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
