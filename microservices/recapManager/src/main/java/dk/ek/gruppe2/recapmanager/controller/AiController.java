package dk.ek.gruppe2.recapmanager.controller;

import dk.ek.gruppe2.recapmanager.dto.AiRequestDTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseDTO;
import dk.ek.gruppe2.recapmanager.dto.AiRequestV2DTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO;
import dk.ek.gruppe2.recapmanager.service.AiService;
import dk.ek.gruppe2.recapmanager.service.RecapService;
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
    private final RecapService recapService;
    public AiController(AiService aiService, RecapService recapService) {
        this.aiService = aiService;
        this.recapService = recapService;
    }

    @PostMapping(path = "/ask", version = "1")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @characterAuthorizationService.canAccessCharacter(#request.characterId, authentication))")
    @Operation(
        summary = "Ask the AI",
        description = "Returns the requested recap or summary. If new journey entries are available from StoryPathManager, combines them with the saved narrative, saves the updated narrative, and returns it. If there are no new entries, returns the saved narrative unchanged. Formats: CHARACTER_RECAP, PATH_SUMMARY."
    )
    public AiResponseDTO ask(@Valid @RequestBody AiRequestDTO request) {
        return new AiResponseDTO(aiService.handleRequest(request));
    }

    @PostMapping(path = "/ask", version = "2")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @characterAuthorizationService.canAccessCharacter(#request.characterId, authentication))")
    @Operation(summary = "Ask the AI with optional narration",
            description = "Returns the requested recap or summary, updating it first when new journey entries exist. "
                    + "With audio=true, retrieves cached speech or requests generation within a bounded wait. "
                    + "Returns the narrative with PENDING if audio is still generating. Formats: CHARACTER_RECAP, PATH_SUMMARY.")
    public AiResponseV2DTO askV2(@Valid @RequestBody AiRequestV2DTO request) {
        return recapService.latest(request);
    }

}
