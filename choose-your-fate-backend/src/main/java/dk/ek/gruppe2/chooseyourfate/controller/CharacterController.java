package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CharacterViewResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateCharacterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.MultipleCharacterViewsResponseDto;
import dk.ek.gruppe2.chooseyourfate.service.CharacterService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/choose-your-fate/characters")
public class CharacterController {
    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping
    public List<CharacterResponseDTO> getAllCharacters(
    ) {
        return characterService.getAllCharacters();
    }

    @GetMapping("/{id}")
    public CharacterResponseDTO getCharacterById(
            @PathVariable Integer id
    ) {
        
        return characterService.getCharacterById(id);
    }

    // Returns the character screen view with character, chapter, race, stats, and creation-limit info together.
    @GetMapping("/all/view")
    public MultipleCharacterViewsResponseDto getCharacterViewById(@RequestBody Integer accountId) {
        return characterService.getCharactersViewBy_AccountId(accountId);
    }

    @PostMapping
    public CharacterResponseDTO createCharacter(
        @RequestBody CreateCharacterRequestDTO request, Integer accountId) {
        return characterService.createCharacter(accountId, request);
    }

    @DeleteMapping("/{id}")
    public void deleteCharacter(@PathVariable Integer id) {
        characterService.deleteCharacter(id);
    }
    
    @GetMapping("/all")
    public List<CharacterResponseDTO> getCharactersByAccountId(@RequestBody Integer accountId) {
        return characterService.getCharactersByAccountId(accountId);
    }
}