package dk.ek.gruppe2.recapmanager;

import dk.ek.gruppe2.recapmanager.ai.AiTools;
import dk.ek.gruppe2.recapmanager.config.*;
import dk.ek.gruppe2.recapmanager.dto.*;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.exception.*;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.service.*;
import dk.ek.gruppe2.recapmanager.storypath.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static dk.ek.gruppe2.recapmanager.enums.AiRequestType.*;

class AiServiceTest {
    final CharacterPathService store = mock(CharacterPathService.class);
    final JourneyHistoryClient history = mock(JourneyHistoryClient.class);
    final CharacterAuthorizationService authorization = mock(CharacterAuthorizationService.class);
    final StoryPathProperties properties = new StoryPathProperties();
    final AiDeploymentProperties aiProperties = new AiDeploymentProperties();
    final ToolCallingTestModel model = new ToolCallingTestModel();
    final AiTools tools = new AiTools(store, history, authorization);
    final AiService service = new AiService(ChatClient.builder(model), tools, aiProperties, properties);
    final SummarySnapshot current = new SummarySnapshot(1, 42, PATH_SUMMARY, "Existing summary", 7, 3L);

    @BeforeEach
    void defaults() {
        when(store.load(42, PATH_SUMMARY)).thenReturn(current);
        when(store.updateCharacterPath(any(), any(), anyLong())).thenAnswer(call -> {
            SummarySnapshot old = call.getArgument(0);
            UpdateCharacterPathRequestDTO update = call.getArgument(1);
            return new CharacterPathResponseDTO(1, old.characterId(), update.getSummary());
        });
    }

    @Test
    void noNewEntriesReturnsExactSavedTextWithoutGenerationOrWrites() {
        var exact = new SummarySnapshot(1, 42, PATH_SUMMARY, "  untouched\nsummary  ", 7, 3L);
        when(store.load(42, PATH_SUMMARY)).thenReturn(exact);
        when(history.fetchAfter(42, 7)).thenReturn(List.of());
        aiProperties.setEnabled(false);
        assertThat(service.handleRequest(request(PATH_SUMMARY))).isEqualTo(exact.summary());
        assertThat(model.calls).isZero();
        verifyNoWrites();
    }

    @Test
    void firstRequestUsesRegisteredToolsAndCreatesCheckpoint() {
        var empty = SummarySnapshot.empty(42, PATH_SUMMARY);
        var entries = List.of(choice(1), choice(2));
        when(store.load(42, PATH_SUMMARY)).thenReturn(empty);
        when(history.fetchAfter(42, 0)).thenReturn(entries);
        model.behavior = callbacks -> {
            assertThat(callbacks.callbacks).containsKeys("getCharacterInfo", "getCharacterPathHistory", "updateCharacterPathSummary");
            assertThat(callbacks.callbacks.get("updateCharacterPathSummary").getToolDefinition().inputSchema())
                    .doesNotContain("toolContext", "lastProcessedSequence", AiTools.SUMMARY_CONTEXT);
            return callbacks.generate("First summary");
        };
        assertThat(service.handleRequest(request(PATH_SUMMARY))).isEqualTo("First summary");
        assertThat(model.histories.getFirst()).contains("(none)", "Sequence 1", "Sequence 2");
        verifySaved(empty, "First summary", 2);
    }

    @Test
    void existingSummaryAndOnlyNewEventsProduceOneAtomicUpdate() {
        when(history.fetchAfter(42, 7)).thenReturn(List.of(choice(8), choice(9)));
        model.behavior = callbacks -> callbacks.generate("Combined summary");
        assertThat(service.handleRequest(request(PATH_SUMMARY))).isEqualTo("Combined summary");
        assertThat(model.histories.getFirst()).contains("Existing summary", "Sequence 8", "Sequence 9");
        verifySaved(current, "Combined summary", 9);
        verify(history, times(1)).fetchAfter(42, 7);
    }

    @Test
    void emptyHistoryWithNoSavedSummaryDoesNotCreateAnEmptyDatabaseRow() {
        when(store.load(42, PATH_SUMMARY)).thenReturn(SummarySnapshot.empty(42, PATH_SUMMARY));
        when(history.fetchAfter(42, 0)).thenReturn(List.of());
        assertThat(service.handleRequest(request(PATH_SUMMARY))).isEmpty();
        assertThat(model.calls).isZero();
        verifyNoWrites();
    }

    @Test
    void sourceFailureDoesNotPretendThereAreNoNewEvents() {
        when(history.fetchAfter(42, 7)).thenThrow(new StoryPathException("offline"));
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(StoryPathException.class);
        assertThat(model.calls).isZero();
        verifyNoWrites();
    }

    @Test
    void providerFailureAfterToolUpdateDoesNotPersistStagedText() {
        when(history.fetchAfter(42, 7)).thenReturn(List.of(choice(8)));
        model.behavior = callbacks -> {
            callbacks.generate("Draft");
            throw new IllegalStateException("Provider failed after tool invocation");
        };
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        verifyNoWrites();
    }

    @Test
    void missingUpdateToolOrBlankSummaryDoesNotAdvanceCheckpoint() {
        when(history.fetchAfter(42, 7)).thenReturn(List.of(choice(8)));
        model.behavior = callbacks -> { callbacks.history(); return "Text without update tool"; };
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        model.behavior = callbacks -> callbacks.generate(" ");
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        verifyNoWrites();
    }

    @Test
    void largeHistoryUsesBoundedToolContextsAndCommitsOnlyTheFinalSummary() {
        properties.setGenerationBatchSize(2);
        when(history.fetchAfter(42, 7)).thenReturn(List.of(choice(8), choice(9), choice(10)));
        model.behavior = callbacks -> callbacks.generate(model.calls == 1 ? "Intermediate" : "Final");
        assertThat(service.handleRequest(request(PATH_SUMMARY))).isEqualTo("Final");
        assertThat(model.histories.get(0)).contains("Existing summary", "Sequence 8", "Sequence 9").doesNotContain("Sequence 10");
        assertThat(model.histories.get(1)).contains("Intermediate", "Sequence 10").doesNotContain("Sequence 8");
        verifySaved(current, "Final", 10);
        verify(store, times(1)).updateCharacterPath(any(), any(), anyLong());
    }

    @Test
    void failureInLaterModelBatchLeavesEarlierDraftUnpersisted() {
        properties.setGenerationBatchSize(1);
        when(history.fetchAfter(42, 7)).thenReturn(List.of(choice(8), choice(9)));
        model.behavior = callbacks -> {
            if (model.calls == 2) throw new IllegalStateException("failure");
            return callbacks.generate("Draft");
        };
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(AiServiceException.class);
        verifyNoWrites();
    }

    @Test
    void recapHasItsOwnCheckpointIndependentOfPathSummary() {
        when(store.load(42, CHARACTER_RECAP)).thenReturn(new SummarySnapshot(2, 42, CHARACTER_RECAP, "Recap", 2, 0L));
        when(history.fetchAfter(42, 2)).thenReturn(List.of());
        assertThat(service.handleRequest(request(CHARACTER_RECAP))).isEqualTo("Recap");
        verify(history).fetchAfter(42, 2);
        verify(store, never()).load(42, PATH_SUMMARY);
    }

    @Test
    void unorderedOrReplayedEntriesAreRejectedBeforeGeneration() {
        for (var entries : List.of(List.of(choice(7)), List.of(choice(9), choice(8)), List.of(choice(8), choice(8)))) {
            when(history.fetchAfter(42, 7)).thenReturn(entries);
            assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(StoryPathException.class);
        }
        assertThat(model.calls).isZero();
        verifyNoWrites();
    }

    @Test
    void unauthorizedRequestsCannotReadLocalOrRemoteData() {
        doThrow(new AccessDeniedException("denied")).when(authorization).requireAccessToCharacter(42);
        assertThatThrownBy(() -> service.handleRequest(request(PATH_SUMMARY))).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store, history);
        assertThat(model.calls).isZero();
    }

    private void verifySaved(SummarySnapshot expected, String summary, long sequence) {
        verify(store).updateCharacterPath(eq(expected), argThat(update -> summary.equals(update.getSummary())), eq(sequence));
    }

    private void verifyNoWrites() { verify(store, never()).updateCharacterPath(any(), any(), anyLong()); }

    private AiRequestDTO request(AiRequestType type) {
        var request = new AiRequestDTO();
        request.setCharacterId(42);
        request.setRequestType(type);
        return request;
    }

    private JourneyChoice choice(long sequence) {
        return new JourneyChoice(sequence, 10, "Village", "Bridge", "Helped a traveler", "Gained an ally", 5);
    }
}
