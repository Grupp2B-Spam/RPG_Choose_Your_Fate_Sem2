package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterDetailsResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateCharacterDetailsRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterDetails;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterDetailsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterDetailsService {

    private final CharacterDetailsRepository characterDetailsRepository;

    public CharacterDetailsService(CharacterDetailsRepository characterDetailsRepository) {
        this.characterDetailsRepository = characterDetailsRepository;
    }

    public List<CharacterDetailsResponseDTO> getAllCharacterDetails() {
        return characterDetailsRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public CharacterDetailsResponseDTO getCharacterDetailsByCharacterId(Integer characterId) {
        return toDto(getCharacterDetailsEntity(characterId));
    }

    public CharacterDetailsResponseDTO updateCharacterDetails(Integer characterId, UpdateCharacterDetailsRequestDTO request) {
        CharacterDetails details = getCharacterDetailsEntity(characterId);
        details.setIntelligence(request.getIntelligence());
        details.setCharisma(request.getCharisma());
        details.setFashion(request.getFashion());
        return toDto(characterDetailsRepository.save(details));
    }

    private CharacterDetails getCharacterDetailsEntity(Integer characterId) {
        return characterDetailsRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character details not found for character id: " + characterId));
    }

    private CharacterDetailsResponseDTO toDto(CharacterDetails details) {
        return new CharacterDetailsResponseDTO(
                details.getCharacterId(),
                details.getIntelligence(),
                details.getCharisma(),
                details.getFashion()
        );
    }
}
