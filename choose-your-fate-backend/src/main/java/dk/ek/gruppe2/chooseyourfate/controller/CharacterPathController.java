package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterPathResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPathChoiceId;
import dk.ek.gruppe2.chooseyourfate.service.CharacterPathService;

import dk.ek.gruppe2.chooseyourfate.service.TTSService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/choose-your-fate/character-paths")
public class CharacterPathController {
    private final CharacterPathService characterPathService;
    private final TTSService ttsService;

    public CharacterPathController(CharacterPathService characterPathService, TTSService ttsService) {
        this.characterPathService = characterPathService;
        this.ttsService = ttsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CharacterPathResponseDTO> getAllCharacterPaths() {
        return characterPathService.getAllCharacterPaths();
    }

    @GetMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public CharacterPathResponseDTO getCharacterPathByCharacterId(
            @PathVariable Integer characterId
    ) {
        return characterPathService.getCharacterPathByCharacterId(characterId);
    }

    @GetMapping("/{characterId}/audio")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable Integer characterId) {
        byte[] bytes = ttsService.textToSpeech(characterId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"speech.mp3\"")
                .body(bytes);
    }

    @PutMapping("/{characterId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public CharacterPathResponseDTO updateCharacterPath(
            @PathVariable Integer characterId,
            @RequestBody UpdateCharacterPathRequestDTO request
    ) {
        return characterPathService.updateCharacterPath(characterId, request);
    }

    @PutMapping("/{characterId}/chosen/{choiceId}")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#characterId, authentication)")
    public CharacterPathChoiceId updateCharacterPathChoices(
            @PathVariable Integer characterId,
            @PathVariable Integer choiceId
    ) {
        return characterPathService.updateCharacterPathChoice(characterId, choiceId);
    }
}