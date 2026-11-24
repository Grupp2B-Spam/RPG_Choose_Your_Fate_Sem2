package dk.ek.gruppe2.recapmanager.unit;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.dto.AiRequestV2DTO;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO.AudioStatus;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.*;
import dk.ek.gruppe2.recapmanager.service.*;
import dk.ek.gruppe2.recapmanager.speech.*;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecapServiceTest {
    private final AiService ai = mock(AiService.class);
    private final SpeechClient speech = mock(SpeechClient.class);
    private final SpeechProperties properties = new SpeechProperties();
    private final RecapService service = new RecapService(ai, speech, properties);

    private AiRequestV2DTO request(boolean audio) {
        var request = new AiRequestV2DTO();
        request.setCharacterId(1);
        request.setRequestType(AiRequestType.PATH_SUMMARY);
        request.setAudio(audio);
        return request;
    }

    @Test void textOnlyPreservesTheRequestedFormatAndDoesNotQuerySpeech() {
        var request = request(false);
        request.setRequestType(AiRequestType.CHARACTER_RECAP);
        when(ai.handleRequest(any())).thenAnswer(call -> {
            AiRequestV2DTO received = call.getArgument(0);
            assertThat(received.getRequestType()).isEqualTo(AiRequestType.CHARACTER_RECAP);
            assertThat(received.getCharacterId()).isEqualTo(1);
            return "You travelled";
        });
        var result = service.latest(request);
        assertThat(result.response()).isEqualTo("You travelled");
        assertThat(result.audioStatus()).isEqualTo(AudioStatus.NOT_REQUESTED);
        verifyNoInteractions(speech);
    }

    @Test void emptyHistoryDoesNotGenerateEmptyAudio() {
        when(ai.handleRequest(any())).thenReturn("");
        assertThat(service.latest(request(true)).audioStatus()).isEqualTo(AudioStatus.NO_TEXT);
        verifyNoInteractions(speech);
    }

    @Test void pendingKeepsTheCurrentText() {
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenAnswer(call -> {
            SpeechRequest request = call.getArgument(0);
            return SpeechResponse.newBuilder().setContentHash(request.getContentHash())
                    .setStatus(SpeechStatus.PENDING).build();
        });
        var result = service.latest(request(true));
        assertThat(result.audioStatus()).isEqualTo(AudioStatus.PENDING);
        assertThat(result.response()).isEqualTo("A journey");
        assertThat(result.audioUrl()).isNull();
    }

    @Test void readyAudioUsesTheExactNarrativeAndConfiguredSynthesisIdentity() {
        properties.setVoiceId("configured-voice");
        properties.setModel("configured-model");
        properties.setOutputFormat("configured-format");
        String expectedHash = SpeechContent.hash("A journey", "configured-voice", "configured-model", "configured-format");
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenAnswer(call -> {
            SpeechRequest request = call.getArgument(0);
            assertThat(request.getText()).isEqualTo("A journey");
            assertThat(request.getVoiceId()).isEqualTo("configured-voice");
            assertThat(request.getModel()).isEqualTo("configured-model");
            assertThat(request.getOutputFormat()).isEqualTo("configured-format");
            assertThat(request.getContentHash()).isEqualTo(expectedHash);
            return SpeechResponse.newBuilder().setContentHash(request.getContentHash())
                    .setStatus(SpeechStatus.READY).setAudioUrl("https://audio.example.test/a.mp3").build();
        });
        var result = service.latest(request(true));
        assertThat(result.audioStatus()).isEqualTo(AudioStatus.READY);
        assertThat(result.response()).isEqualTo("A journey");
        assertThat(result.contentHash()).isEqualTo(expectedHash);
        assertThat(result.audioUrl()).isEqualTo("https://audio.example.test/a.mp3");
    }

    @Test void staleAudioIsNeverReturned() {
        when(ai.handleRequest(any())).thenReturn("A new journey");
        when(speech.getOrCreate(any())).thenReturn(SpeechResponse.newBuilder().setContentHash("old")
                .setStatus(SpeechStatus.READY).setAudioUrl("https://audio.example.test/old.mp3").build());
        var result = service.latest(request(true));
        assertThat(result.audioStatus()).isEqualTo(AudioStatus.UNAVAILABLE);
        assertThat(result.audioUrl()).isNull();
        assertThat(result.response()).isEqualTo("A new journey");
    }

    @Test void deadlineReturnsTextWithPendingAudio() {
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenThrow(Status.DEADLINE_EXCEEDED.asRuntimeException());
        assertThat(service.latest(request(true)).audioStatus()).isEqualTo(AudioStatus.PENDING);
    }

    @Test void speechOutageDoesNotFailSavedText() {
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenThrow(Status.UNAVAILABLE.asRuntimeException());
        var result = service.latest(request(true));
        assertThat(result.audioStatus()).isEqualTo(AudioStatus.UNAVAILABLE);
        assertThat(result.response()).isEqualTo("A journey");
    }

    @Test void speechAuthenticationFailureIsNotHiddenAsAnAudioOutage() {
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenThrow(new InvalidBearerTokenException("Rejected"));
        assertThatThrownBy(() -> service.latest(request(true))).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test void speechPermissionFailureIsNotHiddenAsAnAudioOutage() {
        when(ai.handleRequest(any())).thenReturn("A journey");
        when(speech.getOrCreate(any())).thenThrow(new AccessDeniedException("Denied"));
        assertThatThrownBy(() -> service.latest(request(true))).isInstanceOf(AccessDeniedException.class);
    }

    @Test void cacheIdentityIncludesEverySynthesisOptionAndHasUnambiguousBoundaries() {
        String hash = SpeechContent.hash("journey", "voice", "model", "mp3");
        assertThat(hash).hasSize(64).isEqualTo(SpeechContent.hash("journey", "voice", "model", "mp3"));
        assertThat(hash).isNotEqualTo(SpeechContent.hash("new journey", "voice", "model", "mp3"))
                .isNotEqualTo(SpeechContent.hash("journey", "other voice", "model", "mp3"))
                .isNotEqualTo(SpeechContent.hash("journey", "voice", "new model", "mp3"))
                .isNotEqualTo(SpeechContent.hash("journey", "voice", "model", "wav"));
        assertThat(SpeechContent.hash("ab", "c", "m", "o"))
                .isNotEqualTo(SpeechContent.hash("a", "bc", "m", "o"));
    }
}
