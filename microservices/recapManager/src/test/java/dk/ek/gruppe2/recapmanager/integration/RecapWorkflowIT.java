package dk.ek.gruppe2.recapmanager.integration;

import dk.ek.gruppe2.recapmanager.ai.AiTools;
import dk.ek.gruppe2.recapmanager.config.*;
import dk.ek.gruppe2.recapmanager.controller.*;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.*;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.*;
import dk.ek.gruppe2.recapmanager.security.*;
import dk.ek.gruppe2.recapmanager.service.*;
import dk.ek.gruppe2.recapmanager.speech.SpeechClient;
import dk.ek.gruppe2.recapmanager.storypath.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP -> services -> registered tools -> database/outbox; only external boundaries are substitutes. */
@SpringJUnitWebConfig(RecapWorkflowIT.Config.class)
@TestPropertySource(properties = {
        "app.security.jwt.mode=keycloak", "app.security.jwt.issuer=https://issuer.test/realms/test",
        "app.security.jwt.audience=recap-api", "app.security.public-docs=false"})
class RecapWorkflowIT {
    private static final String BODY = "{\"characterId\":1,\"requestType\":\"PATH_SUMMARY\",\"audio\":true}";
    @Autowired WebApplicationContext context;
    @Autowired CharacterPathService storage;
    @Autowired CharacterPathRepository paths;
    @Autowired RecapOutboxRepository outbox;
    @Autowired WorkflowModel model;
    @Autowired ObjectMapper mapper;
    @MockitoBean JourneyHistoryClient history;
    @MockitoBean SpeechClient speech;
    @MockitoBean(name = "characterAccessChecker") CharacterAccessChecker ownership;
    private List<JourneyChoice> additions;
    private final List<SpeechRequest> speechRequests = new ArrayList<>();
    private MockMvc mvc;

    @BeforeEach void setup() {
        outbox.deleteAll();
        paths.deleteAll();
        model.calls = 0;
        model.histories.clear();
        speechRequests.clear();
        additions = List.of(choice(1));
        when(ownership.canAccess(1, "player-1")).thenReturn(true);
        when(history.fetchAfter(eq(1), anyLong())).thenAnswer(call -> {
            long cursor = call.getArgument(1);
            return additions.stream().filter(choice -> choice.sequence() > cursor).toList();
        });
        when(speech.getOrCreate(any())).thenAnswer(call -> {
            SpeechRequest request = call.getArgument(0);
            speechRequests.add(request);
            return SpeechResponse.newBuilder().setContentHash(request.getContentHash()).setStatus(SpeechStatus.READY)
                    .setAudioUrl("https://audio.example.test/journey.mp3").build();
        });
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void repeatedRequestReturnsSavedNarrativeWithoutAnotherModelCallOrEvent() throws Exception {
        storage.saveUpdatedSummary(SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY), "Earlier journey", 1);
        outbox.deleteAll(); // The prior narrative's publication has already been handled.
        additions = List.of(choice(2));

        request(200);
        var saved = storage.load(1, AiRequestType.PATH_SUMMARY);
        var row = paths.findByCharacterIdAndRequestType(1, AiRequestType.PATH_SUMMARY);
        var updatedAt = row.getSummaryUpdatedAt();
        var publication = outbox.findAll().getFirst();
        assertThat(saved.summary()).isEqualTo("Updated journey");
        assertThat(saved.lastProcessedSequence()).isEqualTo(2);
        assertThat(model.calls).isEqualTo(1);
        assertThat(model.histories).singleElement().asString().contains("Earlier journey", "Event 2");
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(mapper.readTree(publication.getPayload()).path("text").asString()).isEqualTo(saved.summary());

        request(200);
        assertThat(storage.load(1, AiRequestType.PATH_SUMMARY)).isEqualTo(saved);
        assertThat(paths.findByCharacterIdAndRequestType(1, AiRequestType.PATH_SUMMARY).getSummaryUpdatedAt()).isEqualTo(updatedAt);
        assertThat(model.calls).isEqualTo(1);
        assertThat(outbox.count()).isEqualTo(1);
        var unchangedPublication = outbox.findById(publication.getId()).orElseThrow();
        assertThat(unchangedPublication.getPayload()).isEqualTo(publication.getPayload());
        assertThat(unchangedPublication.getVersion()).isEqualTo(publication.getVersion());
        verify(history).fetchAfter(1, 1);
        verify(history).fetchAfter(1, 2);
        assertThat(speechRequests).hasSize(2);
        String eventHash = mapper.readTree(publication.getPayload()).path("contentHash").asString();
        assertThat(speechRequests).extracting(SpeechRequest::getText).containsExactly(saved.summary(), saved.summary());
        assertThat(speechRequests).extracting(SpeechRequest::getContentHash).containsExactly(eventHash, eventHash);
    }

    @Test void ownershipLookupFailureStopsTheWholeWorkflowBeforeDataRetrieval() {
        when(ownership.canAccess(1, "player-1")).thenThrow(new IllegalStateException("Ownership source unavailable"));
        assertThatThrownBy(() -> request(200)).hasRootCauseInstanceOf(IllegalStateException.class);
        verifyNoInteractions(history, speech);
        assertThat(model.calls).isZero();
        assertThat(paths.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    @Test void speechTokenRejectionReachesHttpWithoutLosingTheCommittedSummary() throws Exception {
        doThrow(new InvalidBearerTokenException("Speech token rejected")).when(speech).getOrCreate(any());
        request(401);
        assertCommittedSummary();
    }

    @Test void speechPermissionDenialReachesHttpWithoutLosingTheCommittedSummary() throws Exception {
        doThrow(new AccessDeniedException("Speech permission denied")).when(speech).getOrCreate(any());
        request(403);
        assertCommittedSummary();
    }

    private void assertCommittedSummary() {
        assertThat(storage.load(1, AiRequestType.PATH_SUMMARY).summary()).isEqualTo("Updated journey");
        assertThat(storage.load(1, AiRequestType.PATH_SUMMARY).lastProcessedSequence()).isEqualTo(1);
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(model.calls).isEqualTo(1);
    }

    private void request(int status) throws Exception {
        var result = mvc.perform(post("/choose-your-fate/ai/ask").header("X-API-VERSION", "2")
                .contentType("application/json").content(BODY)
                .with(jwt().jwt(token -> token.subject("player-1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().is(status));
        if (status == 200) result.andExpect(jsonPath("$.response").value("Updated journey"))
                .andExpect(jsonPath("$.audioStatus").value("READY"))
                .andExpect(jsonPath("$.audioUrl").value("https://audio.example.test/journey.mp3"));
    }

    private JourneyChoice choice(long sequence) {
        return new JourneyChoice(sequence, (int) sequence, "From", "To", "Event " + sequence, "", 1);
    }

    /** Runs the real callbacks registered on ChatClient; the text generator itself is the substitute. */
    static class WorkflowModel implements ChatModel {
        int calls;
        final List<String> histories = new ArrayList<>();
        @Override public ChatResponse call(Prompt prompt) {
            calls++;
            var options = (ToolCallingChatOptions) prompt.getOptions();
            var context = new ToolContext(options.getToolContext());
            histories.add(callback(options, "getCharacterPathHistory").call("{\"characterId\":1}", context));
            callback(options, "updateCharacterPathSummary").call("{\"characterId\":1,\"summary\":\"Updated journey\"}", context);
            return new ChatResponse(List.of(new Generation(new AssistantMessage("Provider final text is not the saved tool update"))));
        }
        private ToolCallback callback(ToolCallingChatOptions options, String name) {
            return options.getToolCallbacks().stream().filter(tool -> tool.getToolDefinition().name().equals(name))
                    .findFirst().orElseThrow();
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({RecapOutboxTransactionIT.Config.class, SecurityConfig.class, ApiVersionConfig.class, RecapExceptionHandler.class})
    static class Config {
        @Bean WorkflowModel model() { return new WorkflowModel(); }
        @Bean CharacterAuthorizationService characterAuthorizationService(CharacterAccessChecker checker) {
            return new CharacterAuthorizationService(checker);
        }
        @Bean AiTools tools(CharacterPathService storage, JourneyHistoryClient history, CharacterAuthorizationService access) {
            return new AiTools(storage, history, access);
        }
        @Bean AiService ai(WorkflowModel model, AiTools tools) {
            return new AiService(ChatClient.builder(model), tools, new AiDeploymentProperties(), new StoryPathProperties());
        }
        @Bean RecapService recaps(AiService ai, SpeechClient speech, SpeechProperties properties) {
            return new RecapService(ai, speech, properties);
        }
        @Bean AiController controller(AiService ai, RecapService recaps) { return new AiController(ai, recaps); }
    }
}
