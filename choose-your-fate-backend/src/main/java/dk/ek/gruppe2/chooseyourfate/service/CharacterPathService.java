package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterPathResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPath;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPathChoice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPathChoiceId;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathChoiceRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChoiceRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterPathService {

    private final CharacterPathRepository characterPathRepository;
    private final ChoiceRepository choiceRepository;
    private final CharacterPathChoiceRepository characterPathChoiceRepository;


    public CharacterPathService(CharacterPathRepository characterPathRepository, ChoiceRepository choiceRepository, CharacterPathChoiceRepository characterPathChoiceRepository) {
        this.characterPathRepository = characterPathRepository;
        this.choiceRepository = choiceRepository;
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
        Choice choice = getChoiceEntity(choiceId);

        CharacterPathChoice characterPathChoice = new CharacterPathChoice(characterPath, choice);
        return characterPathChoiceRepository.save(characterPathChoice).getId();
    }

    private CharacterPath getCharacterPathEntity(Integer characterId) {
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);
        if (characterPath == null) {
            throw new ResourceNotFoundException("Character path not found for character id: " + characterId);
        }
        return characterPath;
    }

    private Choice getChoiceEntity(Integer id) {
        return choiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Choice not found with id: " + id));
    }

    private CharacterPathResponseDTO toDto(CharacterPath characterPath) {
        return new CharacterPathResponseDTO(
                characterPath.getId(),
                characterPath.getCharacter().getId(),
                characterPath.getSummary()
        );
    }
}