package dk.ek.gruppe2.chooseyourfate.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.events.ReplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReplicationEventHandler {

    private final ReplicationService replicationService;

    public ReplicationEventHandler(ReplicationService replicationService) {
        this.replicationService = replicationService;
    }

    @EventListener
    public void handle(ReplicationEvent event) {
        replicationService.createJobAndAddToQueue(event.operationType(), event.entityName(), event.payload());
    }
}
