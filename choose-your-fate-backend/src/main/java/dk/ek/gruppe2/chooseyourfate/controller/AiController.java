package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.AiRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.AiResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/choose-your-fate/ai")
@Tag(name = "AI", description = "Endpoints for AI-generated content using a configured LLM provider")
public class AiController {

    private final AiService aiService;
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/ask")
    @PreAuthorize("hasRole('ADMIN') or @characterAuthorizationService.canAccessCharacter(#request.characterId, authentication)")
    @Operation(
        summary = "Ask the AI",
        description = "Sends a request to the configured AI provider. The model will autonomously call the relevant tools to fetch data and generate a response. Possible request types: CHARACTER_RECAP, PATH_SUMMARY"
    )
    public AiResponseDTO ask(@Valid @RequestBody AiRequestDTO request) {
        return new AiResponseDTO(aiService.handleRequest(request));
    }

}
