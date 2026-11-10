package dk.ek.gruppe2.recapmanager.service;

import dk.ek.gruppe2.recapmanager.dto.CharacterPathResponseDTO;
import dk.ek.gruppe2.recapmanager.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.recapmanager.exception.ResourceNotFoundException;
import dk.ek.gruppe2.recapmanager.model.CharacterPath;
import dk.ek.gruppe2.recapmanager.model.CharacterPathChoice;
import dk.ek.gruppe2.recapmanager.model.CharacterPathChoiceId;
import dk.ek.gruppe2.recapmanager.repository.CharacterPathChoiceRepository;
import dk.ek.gruppe2.recapmanager.repository.CharacterPathRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterPathService {

    private final CharacterPathRepository characterPathRepository;
    private final CharacterPathChoiceRepository characterPathChoiceRepository;


    public CharacterPathService(CharacterPathRepository characterPathRepository, CharacterPathChoiceRepository characterPathChoiceRepository) {
        this.characterPathRepository = characterPathRepository;
        this.characterPathChoiceRepository = characterPathChoiceRepository;
    }

    public List<CharacterPathResponseDTO> getAllCharacterPaths() {
        return characterPathRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CharacterPathResponseDTO getCharacterPathByCharacterId(Integer characterId) {
        return toDto(getCharacterPathEntity(characterId));
    }


    public CharacterPathResponseDTO updateCharacterPath(Integer characterId, UpdateCharacterPathRequestDTO request) {
        CharacterPath characterPath = getCharacterPathEntity(characterId);
        characterPath.setSummary(request.getSummary());
        return toDto(characterPathRepository.save(characterPath));
    }

    public CharacterPathChoiceId updateCharacterPathChoice(Integer characterId, Integer choiceId) {
        CharacterPath characterPath = getCharacterPathEntity(characterId);

        CharacterPathChoice characterPathChoice = new CharacterPathChoice(characterPath, choiceId);
        return characterPathChoiceRepository.save(characterPathChoice).getId();
    }

    private CharacterPath getCharacterPathEntity(Integer characterId) {
        CharacterPath characterPath = characterPathRepository.findByCharacterId(characterId);
        if (characterPath == null) {
            throw new ResourceNotFoundException("Character path not found for character id: " + characterId);
        }
        return characterPath;
    }

    private CharacterPathResponseDTO toDto(CharacterPath characterPath) {
        return new CharacterPathResponseDTO(
                characterPath.getId(),
                characterPath.getCharacterId(),
                characterPath.getSummary()
        );
    }
}
