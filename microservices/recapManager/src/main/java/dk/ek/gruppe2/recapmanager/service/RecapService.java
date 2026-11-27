package dk.ek.gruppe2.recapmanager.service;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.dto.AiRequestV2DTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO.AudioStatus;
import dk.ek.gruppe2.recapmanager.speech.SpeechClient;
import dk.ek.gruppe2.recapmanager.speech.SpeechContent;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class RecapService {
    private static final Logger log = LoggerFactory.getLogger(RecapService.class);
    private final AiService ai;
    private final SpeechClient speech;
    private final SpeechProperties properties;

    public RecapService(AiService ai, SpeechClient speech, SpeechProperties properties) {
        this.ai = ai;
        this.speech = speech;
        this.properties = properties;
    }

    public AiResponseV2DTO latest(AiRequestV2DTO request) {
        int characterId = request.getCharacterId();
        // Reuse authorization, incremental history, generation and persistence without a second save.
        String text = ai.handleRequest(request);
        if (!request.isAudio()) return response(text, AudioStatus.NOT_REQUESTED, null, null);
        if (text.isBlank()) return response(text, AudioStatus.NO_TEXT, null, null);
        var speechRequest = SpeechContent.request(text, properties);
        try {
            var result = speech.getOrCreate(speechRequest);
            String hash = result.getContentHash();
            if (!hash.matches("[0-9a-f]{64}")) {
                throw new IllegalStateException("Speech response requires a valid audio identity");
            }
            return switch (result.getStatus()) {
                case READY -> {
                    URI url = URI.create(result.getAudioUrl());
                    if (url.getHost() == null || !("https".equalsIgnoreCase(url.getScheme())
                            || "http".equalsIgnoreCase(url.getScheme()))) {
                        throw new IllegalStateException("READY speech response requires an HTTP(S) URL");
                    }
                    yield response(text, AudioStatus.READY, url.toString(), hash);
                }
                case PENDING -> response(text, AudioStatus.PENDING, null, hash);
                case FAILED -> response(text, AudioStatus.FAILED, null, hash);
                default -> throw new IllegalStateException("Invalid speech response status");
            };
        } catch (AuthenticationException | AccessDeniedException exception) {
            throw exception;
        } catch (StatusRuntimeException exception) {
            var status = exception.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED
                    ? AudioStatus.PENDING : AudioStatus.UNAVAILABLE;
            return response(text, status, null, null);
        } catch (RuntimeException exception) {
            log.warn("Audio lookup failed for character {} ({})", characterId, exception.getClass().getSimpleName());
            return response(text, AudioStatus.UNAVAILABLE, null, null);
        }
    }

    private AiResponseV2DTO response(String text, AudioStatus status, String url, String hash) {
        return new AiResponseV2DTO(text, status, url, hash);
    }
}
