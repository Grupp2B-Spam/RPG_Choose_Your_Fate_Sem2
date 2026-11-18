package dk.ek.gruppe2.recapmanager.ai;

import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.recapmanager.exception.AiServiceException;
import dk.ek.gruppe2.recapmanager.exception.StoryPathException;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.service.CharacterPathService;
import dk.ek.gruppe2.recapmanager.storypath.JourneyChoice;
import dk.ek.gruppe2.recapmanager.storypath.JourneyHistoryClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiTools {
    public static final String SUMMARY_CONTEXT = "recapSummaryContext";

    private final CharacterPathService characterPathService;
    private final JourneyHistoryClient journeyHistoryClient;
    private final CharacterAuthorizationService characterAuthorizationService;

    public AiTools(CharacterPathService characterPathService, JourneyHistoryClient journeyHistoryClient,
                   CharacterAuthorizationService characterAuthorizationService) {
        this.characterPathService = characterPathService;
        this.journeyHistoryClient = journeyHistoryClient;
        this.characterAuthorizationService = characterAuthorizationService;
    }

    /** Fetch once before calling the model so unchanged summaries can return without an AI call. */
    public PreparedHistory prepareCharacterPathHistory(Integer characterId, AiRequestType format) {
        characterAuthorizationService.requireAccessToCharacter(characterId);
        var saved = characterPathService.load(characterId, format);
        var additions = List.copyOf(journeyHistoryClient.fetchAfter(characterId, saved.lastProcessedSequence()));
        long sequence = saved.lastProcessedSequence();
        for (var choice : additions) {
            if (choice.sequence() <= sequence) {
                throw new StoryPathException("Journey entries must be ordered and newer than the checkpoint");
            }
            sequence = choice.sequence();
        }
        if (saved.lastProcessedSequence() > 0 && (saved.summary() == null || saved.summary().isBlank())) {
            throw new AiServiceException("A processed checkpoint has no summary; repair the saved record before continuing");
        }
        return new PreparedHistory(saved, additions);
    }

    @Tool(description = "Get the requested character ID and latest scene supplied in this journey batch. Biography, race, stats and flags are not supplied by the journey service.")
    public String getCharacterInfo(
            @ToolParam(description = "The ID of the character to retrieve") Integer characterId,
            ToolContext toolContext) {
        var context = contextFor(characterId, toolContext);
        String scene = context.additions.isEmpty() ? "Unknown" : context.additions.getLast().toScene();
        return "Character ID: " + characterId + " | Latest supplied journey scene: " + scene;
    }

    @Tool(description = """
            Get the existing narrative and new journey choices for this character in the current batch.
            Choices were retrieved from StoryPathManager over gRPC and have not yet been included in the saved narrative.
            Each entry includes its sequence, source/destination scene, description, consequence and story weight.
            Call this before generating an updated narrative.
            """)
    public String getCharacterPathHistory(
            @ToolParam(description = "The ID of the character whose journey to retrieve") Integer characterId,
            ToolContext toolContext) {
        var context = contextFor(characterId, toolContext);
        context.historyRead = true;
        String history = context.additions.stream().map(choice -> String.format(
                "- Sequence %d | Choice ID: %d | From '%s' to '%s' | Choice: %s | Consequence: %s | Story weight: %d",
                choice.sequence(), choice.choiceId(), choice.fromScene(), choice.toScene(), choice.description(),
                choice.consequence(), choice.storyWeight())).collect(Collectors.joining("\n"));
        return "Existing summary: " + (context.existingSummary == null ? "(none)" : context.existingSummary)
                + "\nNew journey entries:\n" + history;
    }

    @Tool(description = """
            Update the character's narrative with the complete combined narrative, including the existing text and all new entries.
            Call only after getCharacterPathHistory. The update is staged for this request and saved with its checkpoint
            after every model batch succeeds. Do not submit a partial narrative or only the new entries.
            """)
    public String updateCharacterPathSummary(
            @ToolParam(description = "The ID of the character whose summary to update") Integer characterId,
            @ToolParam(description = "The complete updated narrative") String summary,
            ToolContext toolContext) {
        var context = contextFor(characterId, toolContext);
        if (!context.historyRead) throw new AiServiceException("Read the journey history before updating its summary");
        if (summary == null || summary.isBlank()) throw new AiServiceException("AI returned an empty narrative");
        context.updatedSummary = summary;
        return "Summary update prepared for character " + characterId + "; it will be saved after generation completes.";
    }

    /** Commit only after all tool calls/model batches complete successfully. */
    public String savePreparedSummary(PreparedHistory prepared, String summary) {
        characterAuthorizationService.requireAccessToCharacter(prepared.saved().characterId());
        if (prepared.additions().isEmpty()) throw new AiServiceException("There are no new entries to save");
        var request = new UpdateCharacterPathRequestDTO();
        request.setSummary(summary);
        return characterPathService.updateCharacterPath(prepared.saved(), request,
                prepared.additions().getLast().sequence()).summary();
    }

    private SummaryContext contextFor(Integer characterId, ToolContext toolContext) {
        characterAuthorizationService.requireAccessToCharacter(characterId);
        Object value = toolContext == null ? null : toolContext.getContext().get(SUMMARY_CONTEXT);
        if (!(value instanceof SummaryContext context) || !context.characterId.equals(characterId)) {
            throw new AccessDeniedException("Tool call does not belong to the requested character");
        }
        return context;
    }

    public record PreparedHistory(SummarySnapshot saved, List<JourneyChoice> additions) {}

    /** One instance per model batch, passed internally by Spring AI, never supplied by the model. */
    public static final class SummaryContext {
        private final Integer characterId;
        private final String existingSummary;
        private final List<JourneyChoice> additions;
        private boolean historyRead;
        private String updatedSummary;

        public SummaryContext(Integer characterId, String existingSummary, List<JourneyChoice> additions) {
            this.characterId = characterId;
            this.existingSummary = existingSummary;
            this.additions = List.copyOf(additions);
        }

        public String updatedSummary() {
            if (!historyRead || updatedSummary == null || updatedSummary.isBlank()) {
                throw new AiServiceException("The model must read the journey and call updateCharacterPathSummary before completing");
            }
            return updatedSummary;
        }
    }
}
