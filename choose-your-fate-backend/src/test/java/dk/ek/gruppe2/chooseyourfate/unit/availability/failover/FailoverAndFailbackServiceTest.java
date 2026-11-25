package dk.ek.gruppe2.chooseyourfate.unit.availability.failover;

import dk.ek.gruppe2.chooseyourfate.availability.failback.DataSynchronizationService;
import dk.ek.gruppe2.chooseyourfate.availability.failback.FailbackService;
import dk.ek.gruppe2.chooseyourfate.availability.failover.FailoverService;
import dk.ek.gruppe2.chooseyourfate.availability.health.PrimaryHealthService;
import dk.ek.gruppe2.chooseyourfate.availability.health.SqlHealthProbe;
import dk.ek.gruppe2.chooseyourfate.availability.replication.InMemoryReplicationQueue;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationJob;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationOperationType;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationWorker;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailoverAndFailbackServiceTest {

    @Test
    void failoverServiceSwitchesToSecondaryWhenPrimaryIsUnavailable() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(false), 1);
        FailoverService failoverService = createFailoverService(routingService, healthService, new InMemoryReplicationQueue(), 10);

        // Act / Assert
        assertTrue(failoverService.evaluateAndFailoverIfNeeded());

        // Assert
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.SECONDARY, routingService.activeRole());
    }

    @Test
    void manualFailoverRequiresReplicationQueueToDrainFirst() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(true), 1);
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        queue.addToQueue(new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", 1)));
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, 0);

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class, failoverService::triggerManualFailover);

        // Assert
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(1, queue.pendingJobs().size());
    }

    @Test
    void emergencyFailoverAttemptsToDrainReplicationQueueBeforeSwitching() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(false), 1);
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        queue.addToQueue(new ReplicationJob(ReplicationOperationType.UPDATE, "account", Map.of("id", 1)));
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, 10);

        // Act / Assert
        assertTrue(failoverService.evaluateAndFailoverIfNeeded());

        // Assert
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(0, queue.pendingJobs().size());
        assertEquals(1, queue.completedJobs().size());
    }

    @Test
    void failbackRequiresHealthyPrimaryBeforeMaintenanceStarts() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();

        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(false), 1);
        FailbackService failbackService = new FailbackService(
                routingService,
                healthService,
                new RecordingSyncService()
        );

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class, failbackService::beginManualFailback);

        // Assert
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
    }

    @Test
    void failbackSynchronizesDataAndReturnsToPrimary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();
        RecordingSyncService syncService = new RecordingSyncService();
        FailbackService failbackService = new FailbackService(
                routingService,
                new PrimaryHealthService(new SequenceProbe(true), 1),
                syncService
        );
        // Act
        failbackService.beginManualFailback();
        failbackService.completeManualFailback();
        // Assert
        assertEquals(1, syncService.syncCount);
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.PRIMARY, routingService.activeRole());
    }

    private static class SequenceProbe implements SqlHealthProbe {
        private final Queue<Boolean> results = new ArrayDeque<>();

        SequenceProbe(Boolean... values) {
            results.addAll(java.util.List.of(values));
        }

        @Override
        public boolean isHealthy(DatabaseRole role) {
            return results.isEmpty() || results.remove();
        }
    }

    private static class RecordingSyncService implements DataSynchronizationService {
        private int syncCount;

        @Override
        public void synchronizeSecondaryToPrimary() {
            syncCount++;
        }
    }

    private static FailoverService createFailoverService(DatabaseRoutingService routingService, PrimaryHealthService healthService, InMemoryReplicationQueue queue, int maxDrainJobs) {
        return new FailoverService(routingService, healthService, queue,
                new ReplicationWorker(queue, job -> {
                }, 3, 0),
                maxDrainJobs
        );
    }

    // ---------- BVA for maxDrainJobsBeforeFailover ----------

    @Test
    void manualFailoverWithDrainSmallerThanQueueShouldAbort() {
        // Arrange: drain limit < queue size, drain catches up partially
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(true), 1);
        InMemoryReplicationQueue queue = queueWith(5);
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, 2);

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class, failoverService::triggerManualFailover);

        // Assert: state rolled back, 3 jobs remain in queue (5 - 2 drained)
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(3, queue.pendingJobs().size());
    }

    @Test
    void manualFailoverWithDrainEqualToQueueShouldSucceed() {
        // Arrange: match-boundary — drain limit equals queue size
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(true), 1);
        InMemoryReplicationQueue queue = queueWith(3);
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, 3);

        // Act
        failoverService.triggerManualFailover();

        // Assert: queue empty, failover completed
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(0, queue.pendingJobs().size());
    }

    @Test
    void manualFailoverWithDrainLargerThanQueueShouldSucceed() {
        // Arrange: drain limit exceeds queue — queue is the binding constraint
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(true), 1);
        InMemoryReplicationQueue queue = queueWith(2);
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, 10);

        // Act
        failoverService.triggerManualFailover();

        // Assert
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(0, queue.pendingJobs().size());
    }

    @Test
    void negativeDrainLimitIsCoercedToZeroAndAbortsManualFailoverWithPendingJobs() {
        // Arrange: invalid input (-1) → coerced to 0 → drain processes nothing
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        PrimaryHealthService healthService = new PrimaryHealthService(new SequenceProbe(true), 1);
        InMemoryReplicationQueue queue = queueWith(1);
        FailoverService failoverService = createFailoverService(routingService, healthService, queue, -1);

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class, failoverService::triggerManualFailover);

        // Assert: nothing drained, state restored
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(1, queue.pendingJobs().size());
    }

// ---------- Helper ----------

    private static InMemoryReplicationQueue queueWith(int jobCount) {
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        for (int i = 0; i < jobCount; i++) {
            queue.addToQueue(new ReplicationJob(
                    ReplicationOperationType.CREATE, "account", Map.of("id", i)));
        }
        return queue;
    }

}
