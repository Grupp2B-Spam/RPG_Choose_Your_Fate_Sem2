package dk.ek.gruppe2.recapmanager;

import dk.ek.gruppe2.recapmanager.ai.AiTools;
import dk.ek.gruppe2.recapmanager.exception.AiServiceException;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.service.CharacterPathService;
import dk.ek.gruppe2.recapmanager.storypath.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiToolsTest {
    final CharacterPathService store = mock(CharacterPathService.class);
    final JourneyHistoryClient history = mock(JourneyHistoryClient.class);
    final CharacterAuthorizationService authorization = mock(CharacterAuthorizationService.class);
    final AiTools tools = new AiTools(store, history, authorization);
    final JourneyChoice choice = new JourneyChoice(8, 10, "Village", "Bridge", "Helped a traveler", "Gained an ally", 5);

    @Test
    void historyToolCombinesSavedTextWithOnlyThePreparedNewEntries() {
        var context = context(new AiTools.SummaryContext(42, "Existing narrative", List.of(choice)));
        assertThat(tools.getCharacterPathHistory(42, context))
                .contains("Existing narrative", "Sequence 8", "Village", "Bridge", "Helped a traveler", "Gained an ally");
        verify(authorization).requireAccessToCharacter(42);
        verifyNoInteractions(store, history);
    }

    @Test
    void toolCannotSwitchCharactersEvenWhenCallerOwnsBothCharacters() {
        var context = context(new AiTools.SummaryContext(42, "Saved", List.of(choice)));
        assertThatThrownBy(() -> tools.getCharacterInfo(99, context)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tools.getCharacterPathHistory(99, context)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tools.updateCharacterPathSummary(99, "Forged summary", context))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store, history);
    }

    @Test
    void callsRequireContextAndRecheckAuthorization() {
        assertThatThrownBy(() -> tools.getCharacterPathHistory(42, new ToolContext(Map.of())))
                .isInstanceOf(AccessDeniedException.class);
        doThrow(new AccessDeniedException("denied")).when(authorization).requireAccessToCharacter(42);
        assertThatThrownBy(() -> tools.getCharacterInfo(42,
                context(new AiTools.SummaryContext(42, null, List.of(choice)))))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(store, history);
    }

    @Test
    void updateToolRequiresHistoryReadAndStagesWithoutWriting() {
        var batch = new AiTools.SummaryContext(42, "Existing", List.of(choice));
        var context = context(batch);
        assertThatThrownBy(() -> tools.updateCharacterPathSummary(42, "New", context)).isInstanceOf(AiServiceException.class);
        tools.getCharacterPathHistory(42, context);
        tools.updateCharacterPathSummary(42, "Combined", context);
        assertThat(batch.updatedSummary()).isEqualTo("Combined");
        verifyNoInteractions(store, history);
    }

    @Test
    void requestContextsDoNotShareStagedTextOrReadState() {
        var first = new AiTools.SummaryContext(42, "Original", List.of(choice));
        var second = new AiTools.SummaryContext(42, "Original", List.of(choice));
        tools.getCharacterPathHistory(42, context(first));
        tools.updateCharacterPathSummary(42, "Draft one", context(first));
        assertThatThrownBy(second::updatedSummary).isInstanceOf(AiServiceException.class);
        assertThat(tools.getCharacterPathHistory(42, context(second))).contains("Original").doesNotContain("Draft one");
        verifyNoInteractions(store, history);
    }

    @Test
    void characterInfoUsesAvailableJourneyFacts() {
        assertThat(tools.getCharacterInfo(42, context(new AiTools.SummaryContext(42, null, List.of(choice)))))
                .contains("Character ID: 42", "Bridge");
    }

    private ToolContext context(AiTools.SummaryContext batch) {
        return new ToolContext(Map.of(AiTools.SUMMARY_CONTEXT, batch));
    }
}
