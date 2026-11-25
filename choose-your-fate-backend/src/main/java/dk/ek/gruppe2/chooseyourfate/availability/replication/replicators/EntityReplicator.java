package dk.ek.gruppe2.chooseyourfate.availability.replication.replicators;

import java.util.Map;

/**
 * Strategy interface for entity-specific replication to the secondary database.
 * Each implementation owns the SQL mapping for one entity. The strategy-based
 * gateway dispatches replication jobs to the matching replicator based on entityName().
 */
public interface EntityReplicator {

    String entityName();

    void create(Map<String, Object> payload);

    void update(Map<String, Object> payload);

    void delete(Map<String, Object> payload);
}
