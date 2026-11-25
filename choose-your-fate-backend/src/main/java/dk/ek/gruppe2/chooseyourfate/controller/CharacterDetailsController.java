package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterDetailsResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateCharacterDetailsRequestDTO;
import dk.ek.gruppe2.chooseyourfate.service.CharacterDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/character-details")
public class CharacterDetailsController {

    private final CharacterDetailsService characterDetailsService;

    public CharacterDetailsController(CharacterDetailsService characterDetailsService) {
        this.characterDetailsService = characterDetailsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CharacterDetailsResponseDTO> getAllCharacterDetails(
    ) {
        return characterDetailsService.getAllCharacterDetails();
    }

    @GetMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public CharacterDetailsResponseDTO getCharacterDetailsByCharacterId(
            @PathVariable Integer characterId
    ) {
        return characterDetailsService.getCharacterDetailsByCharacterId(characterId);
    }

    @PutMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public CharacterDetailsResponseDTO updateCharacterDetails(
            @PathVariable Integer characterId,
            @RequestBody UpdateCharacterDetailsRequestDTO request
    ) {
        return characterDetailsService.updateCharacterDetails(characterId, request);
    }
}
