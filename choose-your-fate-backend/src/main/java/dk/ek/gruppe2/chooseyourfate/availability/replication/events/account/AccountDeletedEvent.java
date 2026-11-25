package dk.ek.gruppe2.chooseyourfate.availability.replication.events.account;

import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationOperationType;
import dk.ek.gruppe2.chooseyourfate.availability.replication.events.ReplicationEvent;

import java.util.Map;

public record AccountDeletedEvent(Map<String, Object> payload) implements ReplicationEvent {
    @Override public String entityName() { return "account"; }
    @Override public ReplicationOperationType operationType() { return ReplicationOperationType.DELETE; }
}
