package dk.ek.gruppe2.chooseyourfate.availability.routing;

public enum DatabaseSystemState {
    PRIMARY_ACTIVE,
    SECONDARY_ACTIVE,
    FAILOVER_IN_PROGRESS,
    FAILBACK_IN_PROGRESS
}
