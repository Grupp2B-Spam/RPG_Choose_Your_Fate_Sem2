package dk.ek.gruppe2.chooseyourfate.ai;

import dk.ek.gruppe2.chooseyourfate.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterAvatar;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterAvatarRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathChoiceRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathRepository;

import dk.ek.gruppe2.chooseyourfate.service.CharacterPathService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AiTools {

    private final CharacterAvatarRepository characterAvatarRepository;
    private final CharacterPathService characterPathService;
    private final CharacterPathRepository characterPathRepository;
    private final CharacterPathChoiceRepository characterPathChoiceRepository;

    public AiTools(
            CharacterAvatarRepository characterAvatarRepository,
            CharacterPathService characterPathService,
            CharacterPathRepository characterPathRepository,
            CharacterPathChoiceRepository characterPathChoiceRepository
    ) {
        this.characterAvatarRepository = characterAvatarRepository;
        this.characterPathService = characterPathService;
        this.characterPathRepository = characterPathRepository;
        this.characterPathChoiceRepository = characterPathChoiceRepository;
    }

    @Tool(description = "Get basic information about a character: name, race ID, current chapter ID, current scene ID, and story flag.")
    public String getCharacterInfo(
            @ToolParam(description = "The ID of the character to retrieve") Integer characterId
    ) {
        CharacterAvatar character = characterAvatarRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));
        return String.format(
            "Name: %s | Race ID: %d | Chapter ID: %d | Scene ID: %d | Flag: %s",
            character.getName(),
            character.getRaceDetails().getId(),
            character.getChapter().getId(),
            character.getScene().getId(),
            character.getFlag()
        );
    }

    @Tool(description = """
        Get the full journey history of a character: every choice they made, which scene they came from,
        which scene they moved to, and the consequence of each choice. Also returns any existing summary.
        Use this to understand what the character has experienced so far.
        """)
    public String getCharacterPathHistory(
            @ToolParam(description = "The ID of the character whose journey to retrieve") Integer characterId
    ) {
        var characterPath = characterPathRepository.findByCharacter_Id(characterId);
        if (characterPath == null) {
            return "No path found for character with ID " + characterId;
        }

        var choices = characterPathChoiceRepository.findByCharacterPath_Id(characterPath.getId());
        if (choices.isEmpty()) {
            return "Character has not made any choices yet.";
        }

        String history = choices.stream()
            .map(cpc -> {
                var choice = cpc.getChoice();
                String fromScene = choice.getScene() != null ? choice.getScene().getName() : "Unknown";
                String toScene = choice.getDestinationScene() != null ? choice.getDestinationScene().getName() : "Unknown";
                return String.format(
                    "- From '%s' → '%s' | Choice: %s | Consequence: %s",
                    fromScene,
                    toScene,
                    choice.getDescription(),
                    choice.getConsequence() != null ? choice.getConsequence() : "none"
                );
            })
            .collect(Collectors.joining("\n"));

        String existingSummary = characterPath.getSummary() != null
            ? "\nExisting summary: " + characterPath.getSummary()
            : "\nNo summary exists yet.";

        return history + existingSummary;
    }

    @Tool(description = """
        Save a narrative summary of the character's journey to their character path record.
        Call this after generating the summary based on the path history.
        """)
    public String updateCharacterPathSummary(
            @ToolParam(description = "The ID of the character whose summary to update") Integer characterId,
            @ToolParam(description = "The narrative summary to save") String summary
    ) {
        var request = new UpdateCharacterPathRequestDTO();
        request.setSummary(summary);
        characterPathService.updateCharacterPath(characterId, request);
        return "Summary saved successfully for character " + characterId;
    }

}