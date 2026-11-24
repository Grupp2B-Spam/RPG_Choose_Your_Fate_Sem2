package dk.ek.gruppe2.recapmanager.messaging;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.model.RecapOutboxMessage;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.RecapOutboxRepository;
import dk.ek.gruppe2.recapmanager.speech.SpeechContent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class RecapEventOutbox {
    private final RecapOutboxRepository repository;
    private final SpeechProperties speech;
    private final ObjectMapper mapper;

    public RecapEventOutbox(RecapOutboxRepository repository, SpeechProperties speech, ObjectMapper mapper) {
        this.repository = repository;
        this.speech = speech;
        this.mapper = mapper;
    }

    /** Mandatory transaction: never save the narrative without its corresponding publication intent. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(SummarySnapshot saved) {
        if (saved.requestType() != AiRequestType.PATH_SUMMARY) return;
        var request = SpeechContent.request(saved.summary(), speech);
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();
        var event = new RecapGenerated(eventId, "RecapGenerated", 1, now, saved.characterId(), saved.requestType(),
                saved.lastProcessedSequence(), saved.version(), request.getContentHash(), request.getText(),
                request.getVoiceId(), request.getModel(), request.getOutputFormat());
        repository.save(new RecapOutboxMessage(eventId, mapper.writeValueAsString(event), now));
    }
}
