package dk.ek.gruppe2.chooseyourfate.availability.replication;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

@Service
public class ReplicationService {

    private final ReplicationQueue replicationQueue;
    private final ApplicationEventPublisher eventPublisher;

    public ReplicationService(ReplicationQueue replicationQueue, ApplicationEventPublisher eventPublisher) {
        this.replicationQueue = replicationQueue;
        this.eventPublisher = eventPublisher;
    }

    public ReplicationJob createJobAndAddToQueue(
            ReplicationOperationType operationType,
            String entityName,
            Map<String, Object> payload
    ) {
        ReplicationJob job = new ReplicationJob(operationType, entityName, payload);
        replicationQueue.addToQueue(job);
        eventPublisher.publishEvent(new ReplicationJobQueuedEvent(job));
        return job;
    }
}
