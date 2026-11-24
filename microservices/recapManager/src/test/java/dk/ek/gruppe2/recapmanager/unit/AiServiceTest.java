package dk.ek.gruppe2.recapmanager.unit;

import dk.ek.gruppe2.recapmanager.ai.AiTools;
import dk.ek.gruppe2.recapmanager.config.*;
import dk.ek.gruppe2.recapmanager.dto.*;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.exception.*;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.service.*;
import dk.ek.gruppe2.recapmanager.storypath.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercise recap decisions and real AiTools; replace only external model/history/storage boundaries. */
class AiServiceTest {
    private final CharacterPathService storage = mock(CharacterPathService.class);
    private final JourneyHistoryClient history = mock(JourneyHistoryClient.class);
    private final CharacterAuthorizationService access = mock(CharacterAuthorizationService.class);
    private final ChatClient.Builder builder = mock(ChatClient.Builder.class);
    private final ChatClient client = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec prompt = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
    private final ChatClient.CallResponseSpec completion = mock(ChatClient.CallResponseSpec.class);
    private final AiDeploymentProperties deployment = new AiDeploymentProperties();
    private final StoryPathProperties properties = new StoryPathProperties();
    private final AiTools tools = new AiTools(storage, history, access);
    private final SummarySnapshot saved = new SummarySnapshot(7, 1, AiRequestType.PATH_SUMMARY, "Earlier journey", 2, 3L);
    private final Deque<String> modelReplies = new ArrayDeque<>();
    private final List<String> suppliedHistories = new ArrayList<>();
    private ToolContext context;
    private int modelCalls;
    private int failOnCall = -1;
    private boolean writeSummary = true;
    private AiService service;

    @BeforeEach void setup() {
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenReturn(prompt);
        when(storage.load(1, AiRequestType.PATH_SUMMARY)).thenReturn(saved);
        when(history.fetchAfter(1, 2)).thenReturn(List.of());
        when(storage.updateCharacterPath(any(), any(), anyLong())).thenAnswer(call -> {
            UpdateCharacterPathRequestDTO update = call.getArgument(1);
            return new CharacterPathResponseDTO(7, 1, update.getSummary());
        });
        doAnswer(call -> {
            context = new ToolContext(call.getArgument(0));
            return prompt;
        }).when(prompt).toolContext(anyMap());
        when(prompt.call()).thenAnswer(call -> {
            if (modelCalls++ == failOnCall) throw new IllegalStateException("Model unavailable");
            suppliedHistories.add(tools.getCharacterPathHistory(1, context));
            if (writeSummary) tools.updateCharacterPathSummary(1, modelReplies.removeFirst(), context);
            return completion;
        });
        service = new AiService(builder, tools, deployment, properties);
    }

    @Test void noNewEntriesReturnSavedTextWithoutModelOrWriteEvenWhenGenerationIsDisabled() {
        deployment.setEnabled(false);
        assertThat(service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isEqualTo("Earlier journey");
        verifyNoInteractions(client);
        verify(storage, never()).updateCharacterPath(any(), any(), anyLong());
    }

    @Test void noSavedNarrativeAndNoEntriesReturnEmptyTextWithoutCreatingARow() {
        when(storage.load(1, AiRequestType.PATH_SUMMARY)).thenReturn(SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY));
        when(history.fetchAfter(1, 0)).thenReturn(List.of());
        assertThat(service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isEmpty();
        verifyNoInteractions(client);
        verify(storage, never()).updateCharacterPath(any(), any(), anyLong());
    }

    @Test void savedTextAndNewEntriesReachTheModelAndItsStagedNarrativeAdvancesTheCheckpoint() {
        when(history.fetchAfter(1, 2)).thenReturn(List.of(choice(3), choice(4)));
        modelReplies.add("Complete updated journey");
        assertThat(service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isEqualTo("Complete updated journey");
        assertThat(suppliedHistories).singleElement().asString().contains("Earlier journey", "Event 3", "Event 4");
        verify(storage).updateCharacterPath(eq(saved), argThat(update -> update.getSummary().equals("Complete updated journey")), eq(4L));
    }

    @Test void batchesCarryTheDraftForwardAndPersistOnlyOnceAfterAllBatches() {
        properties.setGenerationBatchSize(1);
        when(history.fetchAfter(1, 2)).thenReturn(List.of(choice(3), choice(4)));
        modelReplies.addAll(List.of("First draft", "Final draft"));
        assertThat(service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isEqualTo("Final draft");
        assertThat(suppliedHistories.getFirst()).contains("Earlier journey", "Event 3").doesNotContain("Event 4");
        assertThat(suppliedHistories.getLast()).contains("First draft", "Event 4").doesNotContain("Event 3");
        verify(storage, times(1)).updateCharacterPath(eq(saved), argThat(update -> update.getSummary().equals("Final draft")), eq(4L));
    }

    @Test void laterBatchFailureDoesNotSaveTheEarlierDraftOrAdvanceTheCheckpoint() {
        properties.setGenerationBatchSize(1);
        when(history.fetchAfter(1, 2)).thenReturn(List.of(choice(3), choice(4)));
        modelReplies.add("First draft");
        failOnCall = 1;
        assertThatThrownBy(() -> service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        assertThat(modelCalls).isEqualTo(2);
        assertThat(suppliedHistories).singleElement().asString().contains("Earlier journey", "Event 3");
        verify(storage, never()).updateCharacterPath(any(), any(), anyLong());
    }

    @Test void aModelThatDoesNotStageASummaryCannotOverwriteSavedText() {
        when(history.fetchAfter(1, 2)).thenReturn(List.of(choice(3)));
        writeSummary = false;
        assertThatThrownBy(() -> service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        verify(storage, never()).updateCharacterPath(any(), any(), anyLong());
    }

    @Test void inaccessibleCharacterIsRejectedBeforeHistoryStorageOrGeneration() {
        doThrow(new AccessDeniedException("Not the owner")).when(access).requireAccessToCharacter(1);
        assertThatThrownBy(() -> service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(storage, history, client);
    }

    @Test void replayedHistoryCannotBeAddedToTheSummaryTwice() {
        when(history.fetchAfter(1, 2)).thenReturn(List.of(choice(3), choice(3)));
        assertThatThrownBy(() -> service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isInstanceOf(StoryPathException.class);
        verifyNoInteractions(client);
        verify(storage, never()).updateCharacterPath(any(), any(), anyLong());
    }

    @Test void recapAndSummaryReadTheirOwnSavedNarratives() {
        var recap = new SummarySnapshot(8, 1, AiRequestType.CHARACTER_RECAP, "Your earlier recap", 2, 0L);
        when(storage.load(1, AiRequestType.CHARACTER_RECAP)).thenReturn(recap);
        assertThat(service.handleRequest(request(AiRequestType.PATH_SUMMARY))).isEqualTo("Earlier journey");
        assertThat(service.handleRequest(request(AiRequestType.CHARACTER_RECAP))).isEqualTo("Your earlier recap");
        verifyNoInteractions(client);
    }

    private JourneyChoice choice(long sequence) {
        return new JourneyChoice(sequence, (int) sequence, "From", "To", "Event " + sequence, "", 1);
    }

    private AiRequestDTO request(AiRequestType type) {
        var request = new AiRequestDTO();
        request.setCharacterId(1);
        request.setRequestType(type);
        return request;
    }
}
