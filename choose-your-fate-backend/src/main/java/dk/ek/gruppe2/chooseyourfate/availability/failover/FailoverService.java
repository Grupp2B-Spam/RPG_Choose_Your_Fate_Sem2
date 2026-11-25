package dk.ek.gruppe2.chooseyourfate.availability.failover;

import dk.ek.gruppe2.chooseyourfate.availability.health.PrimaryHealthService;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationQueue;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationWorker;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// is responsible for the failover flow!
@Service
public class FailoverService {

    private final DatabaseRoutingService databaseRoutingService;
    private final PrimaryHealthService primaryHealthService;
    private final ReplicationQueue replicationQueue;
    private final ReplicationWorker replicationWorker;
    private final int maxDrainJobsBeforeFailover;

    public FailoverService(
            // used to change state
            DatabaseRoutingService databaseRoutingService,
            // used to judge if primary is unavailable
            PrimaryHealthService primaryHealthService,
            // checks if there's still jobs in the queue
            ReplicationQueue replicationQueue,
            // used to attempt to empty the queue before the failover takes effect
            ReplicationWorker replicationWorker,
            // max amount of jobs we want to complete before failover
            @Value("${app.availability.failover.max-drain-jobs:10}") int maxDrainJobsBeforeFailover
    ) {
        this.databaseRoutingService = databaseRoutingService;
        this.primaryHealthService = primaryHealthService;
        this.replicationQueue = replicationQueue;
        this.replicationWorker = replicationWorker;
        this.maxDrainJobsBeforeFailover = Math.max(0, maxDrainJobsBeforeFailover);
    }

//    1. Won't do anything if current state is PRIMARY_ACTIVE.
//    2. Checks primary health.
//    3. If primary is not yet available it does nothing.
//    4. If primary is unavailable rigger failover and return true.
    public boolean evaluateAndFailoverIfNeeded() {
        // will be removed when is system is extended to also implement health checks of the secondary database.
        if (databaseRoutingService.state() != DatabaseSystemState.PRIMARY_ACTIVE) {
            return false;
        }

        primaryHealthService.checkPrimaryHealth();
        if (!primaryHealthService.isPrimaryUnavailable()) {
            return false;
        }

        triggerEmergencyFailover();
        return true;
    }

    // manuel called from the availability controller and failover triggered here is stricter than emergency failover, and will always wait for the queue to empty before triggering.
    public void triggerManualFailover() {
        databaseRoutingService.beginFailover();
        drainReplicationQueue();
        if (!replicationQueue.pendingJobs().isEmpty()) {
            databaseRoutingService.abortFailover();
            throw new InvalidDatabaseStateTransitionException(
                    "Manual failover requires an empty replication queue. Pending jobs: "
                            + replicationQueue.pendingJobs().size()
            );
        }
        databaseRoutingService.completeFailover();
    }
    // Will attempt to empty the jobs queue, but will always failover if a certain amount of time.
    private void triggerEmergencyFailover() {
        databaseRoutingService.beginFailover();
        drainReplicationQueue();
        databaseRoutingService.completeFailover();
    }

    private void drainReplicationQueue() {
        replicationWorker.processBatch(maxDrainJobsBeforeFailover);
    }
}
