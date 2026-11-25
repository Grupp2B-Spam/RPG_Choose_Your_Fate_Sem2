package dk.ek.gruppe2.chooseyourfate.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.replicators.EntityReplicator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy-based dispatcher: routes replication jobs to the EntityReplicator that
 * matches the job's entityName. Jobs for unregistered entities are silently ignored.
 * To add a new entity, implement EntityReplicator and let Spring pick it up
 * via @Component — no changes needed in this class.
 */
@Primary
@Component
public class ReplicationDispatcher implements SecondaryReplicationGateway {

    private final Map<String, EntityReplicator> replicatorsByEntity;

    public ReplicationDispatcher(List<EntityReplicator> replicators) {
        this.replicatorsByEntity = replicators.stream()
                .collect(Collectors.toMap(EntityReplicator::entityName, Function.identity()));
    }

    @Override
    public void apply(ReplicationJob job) {
        EntityReplicator replicator = replicatorsByEntity.get(job.getEntityName());
        if (replicator == null) {
            return; // Out-of-scope entity — no replicator registered
        }
        switch (job.getOperationType()) {
            case CREATE -> replicator.create(job.getPayload());
            case UPDATE -> replicator.update(job.getPayload());
            case DELETE -> replicator.delete(job.getPayload());
        }
    }
}
