package dk.ek.gruppe2.chooseyourfate.availability.routing;

import org.springframework.stereotype.Service;

@Service
public class DatabaseRoutingService {

    private DatabaseSystemState state = DatabaseSystemState.PRIMARY_ACTIVE;
    private DatabaseRole activeRole = DatabaseRole.PRIMARY;
    private boolean maintenanceMode;
    // below methords are synchronized to avoid multiple request changing the same state at the same time.
    // in other words. We only want one thread to read/edit state at the time.
    public synchronized DatabaseRole activeRole() {
        return activeRole;
    }

    public synchronized DatabaseRole routeRead() {
        return activeRole;
    }

    public synchronized DatabaseRole routeWrite() {
        return activeRole;
    }

    public synchronized DatabaseSystemState state() {
        return state;
    }

    public synchronized boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public synchronized void beginFailover() {
        requireState(DatabaseSystemState.PRIMARY_ACTIVE, "Failover can only start while state is PRIMARY_ACTIVE");
        state = DatabaseSystemState.FAILOVER_IN_PROGRESS;
    }

    public synchronized void completeFailover() {
        requireState(DatabaseSystemState.FAILOVER_IN_PROGRESS, "Failover can only complete while state is FAILOVER_IN_PROGRESS");
        activeRole = DatabaseRole.SECONDARY;
        state = DatabaseSystemState.SECONDARY_ACTIVE;
    }

    public synchronized void abortFailover() {
        requireState(DatabaseSystemState.FAILOVER_IN_PROGRESS, "Failover can only be aborted while state is FAILOVER_IN_PROGRESS");
        activeRole = DatabaseRole.PRIMARY;
        state = DatabaseSystemState.PRIMARY_ACTIVE;
    }

    public synchronized void beginFailback() {
        requireState(DatabaseSystemState.SECONDARY_ACTIVE, "Failback can only start while state is SECONDARY_ACTIVE");
        maintenanceMode = true;
        state = DatabaseSystemState.FAILBACK_IN_PROGRESS;
    }

    public synchronized void completeFailback() {
        requireState(DatabaseSystemState.FAILBACK_IN_PROGRESS, "Failback can only complete while state is FAILBACK_IN_PROGRESS");
        activeRole = DatabaseRole.PRIMARY;
        maintenanceMode = false;
        state = DatabaseSystemState.PRIMARY_ACTIVE;
    }

    public synchronized void abortFailback() {
        requireState(DatabaseSystemState.FAILBACK_IN_PROGRESS, "Failback can only be aborted while state is FAILBACK_IN_PROGRESS");
        maintenanceMode = false;
        activeRole = DatabaseRole.SECONDARY;
        state = DatabaseSystemState.SECONDARY_ACTIVE;
    }

    private void requireState(DatabaseSystemState expected, String message) {
        if (state != expected) {
            throw new InvalidDatabaseStateTransitionException(message + ". Current state: " + state);
        }
    }
}
