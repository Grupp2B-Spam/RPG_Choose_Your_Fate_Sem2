package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CharacterStatsDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CharacterViewResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateCharacterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.MultipleCharacterViewsResponseDto;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.CharacterAvatar;
import dk.ek.gruppe2.chooseyourfate.model.CharacterDetails;
import dk.ek.gruppe2.chooseyourfate.model.RaceDetails;
import dk.ek.gruppe2.chooseyourfate.repository.CharacterAvatarRepository;
import dk.ek.gruppe2.chooseyourfate.repository.CharacterDetailsRepository;
import dk.ek.gruppe2.chooseyourfate.repository.RaceDetailsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CharacterService {
    private EntityManager entityManager;

    private final CharacterAvatarRepository characterAvatarRepository;
    private final RaceDetailsRepository raceDetailsRepository;
    private final CharacterDetailsRepository characterDetailsRepository;

    public CharacterService(
            CharacterAvatarRepository characterAvatarRepository,
            RaceDetailsRepository raceDetailsRepository,
            EntityManager entityManager,
            CharacterDetailsRepository characterDetailsRepository
    ) {
        this.characterAvatarRepository = characterAvatarRepository;
        this.characterDetailsRepository = characterDetailsRepository;
        this.raceDetailsRepository = raceDetailsRepository;
        this.entityManager = entityManager;
    }

    public List<CharacterResponseDTO> getAllCharacters() {
        return characterAvatarRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CharacterResponseDTO getCharacterById(Integer id) {
        return toDto(getCharacterEntity(id));
    }

    // Builds one SQL-backed character view so the frontend does not need separate character and detail requests.
    public MultipleCharacterViewsResponseDto getCharactersViewBy_AccountId(Integer id) {
        long characterCount = characterAvatarRepository.countByAccountId(id);

        List<CharacterAvatar> characters = getCharacterentitesByAccountId(id);
        
        List<CharacterViewResponseDTO> views = new ArrayList<>();

        for (CharacterAvatar character : characters) {
            CharacterDetails details = getCharacterDetailsEntity(character.getId());
            views.add(toViewDto(character, details));
        }

        return new MultipleCharacterViewsResponseDto(views);
    }

    public CharacterResponseDTO createCharacter(Integer accountId, CreateCharacterRequestDTO request) {
        if (request.getChapterId() == null || request.getSceneId() == null) {
            RaceDetails raceDetails = raceDetailsRepository.findById(request.getRaceDetailsId())
                    .orElseThrow(() -> new ResourceNotFoundException("Race details not found with id: " + request.getRaceDetailsId()));

            Integer startingChapter = raceDetails.getStartingChapter();
            if (startingChapter == null) {
                throw new ResourceNotFoundException("Starting chapter not configured for race details with id: " + request.getRaceDetailsId());
            }

            Integer startingScene = raceDetails.getStartingScene();
            if (startingScene == null) {
                throw new ResourceNotFoundException("Starting scene not configured for race details with id: " + request.getRaceDetailsId());
            }

            request.setChapterId(startingChapter);
            request.setSceneId(startingScene);
        }

        validateCreateRequest(request);

        StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("sp_create_character");
        storedProcedure.registerStoredProcedureParameter("p_account_id", Integer.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("p_chapter_id", Integer.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("p_scene_id", Integer.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("p_race_detail_id", Integer.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("p_name", String.class, ParameterMode.IN);
        storedProcedure.registerStoredProcedureParameter("p_character_id", Integer.class, ParameterMode.OUT);

        storedProcedure.setParameter("p_account_id", accountId);
        storedProcedure.setParameter("p_chapter_id", request.getChapterId());
        storedProcedure.setParameter("p_scene_id", request.getSceneId());
        storedProcedure.setParameter("p_race_detail_id", request.getRaceDetailsId());
        storedProcedure.setParameter("p_name", request.getName());

        storedProcedure.execute();

        Integer createdCharacterId = (Integer) storedProcedure.getOutputParameterValue("p_character_id");
        return toDto(getCharacterEntity(createdCharacterId));
    }

    public void deleteCharacter(Integer id) {
        if (!characterAvatarRepository.existsById(id)) {
            throw new ResourceNotFoundException("Character not found with id: " + id);
        }

        StoredProcedureQuery storedProcedure = entityManager.createStoredProcedureQuery("sp_delete_character");
        storedProcedure.registerStoredProcedureParameter("p_character_id", Integer.class, ParameterMode.IN);
        storedProcedure.setParameter("p_character_id", id);
        storedProcedure.execute();
    }

    //Made to retreive all characters connected to the account that is logged in.
    public List<CharacterResponseDTO> getCharactersByAccountId(Integer id) {
        return getCharacterentitesByAccountId(id).stream()
                .map(this::toDto)
                .toList();
    }

    private List<CharacterAvatar> getCharacterentitesByAccountId(Integer id) {
        return characterAvatarRepository.findByAccountId(id);
    }

    private CharacterAvatar getCharacterEntity(Integer id) {
        return characterAvatarRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + id));
    }

    // Loads the auto-created stats row for a character.
    private CharacterDetails getCharacterDetailsEntity(Integer characterId) {
        return characterDetailsRepository.findByIdWithCharacterView(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character details not found for character id: " + characterId));
    }

    // In this SQL implementation, referenced entities are validated before insert so the service fails fast with clear errors
    // and avoids database-level foreign key violations. This method also enforces domain-specific consistency, such as
    // ensuring that the selected scene belongs to the selected chapter.
    private void validateCreateRequest(CreateCharacterRequestDTO request) {
        if (!raceDetailsRepository.existsById(request.getRaceDetailsId())) {
            throw new ResourceNotFoundException("Race details not found with id: " + request.getRaceDetailsId());
        }
    }

    private CharacterResponseDTO toDto(CharacterAvatar character) {
        return new CharacterResponseDTO(
                character.getId(),
                character.getAccount(),
                character.getChapter(),
                character.getScene(),
                character.getRaceDetails().getId(),
                character.getName(),
                character.getFlag()
        );
    }


    // Maps character and detail data to the combined character view response.
    private CharacterViewResponseDTO toViewDto(
            CharacterAvatar character,
            CharacterDetails details
    ) {
        return new CharacterViewResponseDTO(
                character.getId(),
                character.getName(),
                character.getChapter(),
                character.getRaceDetails().getId(),
                character.getRaceDetails().getName(),
                new CharacterStatsDTO(
                        details.getIntelligence(),
                        details.getCharisma(),
                        details.getFashion()
                )
        );
    }
}
