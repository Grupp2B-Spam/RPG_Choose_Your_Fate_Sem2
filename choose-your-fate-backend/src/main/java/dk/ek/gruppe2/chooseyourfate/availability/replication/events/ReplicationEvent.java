package dk.ek.gruppe2.chooseyourfate.availability.replication.events;

import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationOperationType;

import java.util.Map;

public interface ReplicationEvent {
    String entityName();
    ReplicationOperationType operationType();
    Map<String, Object> payload();
}