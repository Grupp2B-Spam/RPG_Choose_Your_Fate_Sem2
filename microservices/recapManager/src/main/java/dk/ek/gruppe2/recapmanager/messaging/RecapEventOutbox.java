package dk.ek.gruppe2.recapmanager.messaging;

import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.model.RecapOutboxMessage;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.RecapOutboxRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class RecapEventOutbox {
    private final RecapOutboxRepository repository;
    private final ObjectMapper mapper;

    public RecapEventOutbox(RecapOutboxRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Mandatory transaction: never save the narrative without its corresponding publication intent. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(SummarySnapshot saved) {
        if (saved.requestType() != AiRequestType.PATH_SUMMARY) return;
        Instant now = Instant.now();
        String eventId = UUID.randomUUID().toString();
        var event = new RecapGenerated(eventId, "RecapGenerated", 2, now, saved.characterId(), saved.requestType(),
                saved.lastProcessedSequence(), saved.version(), saved.summary());
        repository.save(new RecapOutboxMessage(eventId, mapper.writeValueAsString(event), now));
    }
}
