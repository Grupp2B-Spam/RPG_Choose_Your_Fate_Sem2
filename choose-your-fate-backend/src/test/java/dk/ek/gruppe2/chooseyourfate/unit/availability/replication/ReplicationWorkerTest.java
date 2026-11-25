package dk.ek.gruppe2.chooseyourfate.unit.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReplicationWorkerTest {

    @Test
    void successfulJobIsAppliedAndMarkedCompleted() {
        // Arrange
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        ReplicationJob job = new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", 1));
        queue.addToQueue(job);

        // Act / Assert
        assertTrue(worker.processNext());

        // Assert
        verify(gateway).apply(job);
        assertEquals(ReplicationStatus.COMPLETED, job.getStatus());
        assertEquals(1, queue.completedJobs().size());
        assertEquals(0, queue.pendingJobs().size());
    }

    @Test
    void failedJobIsRequeuedUntilItCanSucceed() {
        // Arrange
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        ReplicationJob job = new ReplicationJob(ReplicationOperationType.UPDATE, "account", Map.of("id", 1));
        doThrow(new IllegalStateException("secondary unavailable"))
                .doNothing()
                .when(gateway)
                .apply(job);
        queue.addToQueue(job);

        // Act
        worker.processNext();

        // Assert
        assertEquals(1, job.getRetryCount());
        assertEquals(ReplicationStatus.PENDING, job.getStatus());
        assertEquals(1, queue.pendingJobs().size());

        // Act
        worker.processNext();

        // Assert
        assertEquals(ReplicationStatus.COMPLETED, job.getStatus());
        assertEquals(1, queue.completedJobs().size());
    }

    @Test
    void failedJobMovesToDeadLetterAfterRetryLimit() {
        // Arrange
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 2, 0);
        ReplicationJob job = new ReplicationJob(ReplicationOperationType.DELETE, "account", Map.of("id", 1));
        doThrow(new IllegalStateException("secondary unavailable")).when(gateway).apply(job);
        queue.addToQueue(job);

        // Act
        worker.processNext();
        worker.processNext();

        // Assert
        assertEquals(ReplicationStatus.DEAD_LETTER, job.getStatus());
        assertEquals(2, job.getRetryCount());
        assertEquals(1, queue.deadLetterJobs().size());
        assertEquals(0, queue.pendingJobs().size());
    }

    // ---------- BVA  on processBatch(maxJobs) ----------

    @Test
    void processBatchWithZeroMaxJobsProcessesNothing() {
        // Arrange
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        queue.addToQueue(new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", 1)));

        // Act
        int processed = worker.processBatch(0);

        // Assert: nul-grænsen returnerer altid 0, queue er urørt
        assertEquals(0, processed);
        assertEquals(1, queue.pendingJobs().size());
    }

    @Test
    void processBatchStopsAtMaxJobsWhenQueueHasMore() {
        // Arrange: maxJobs er bindende grænse
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        for (int i = 0; i < 5; i++) {
            queue.addToQueue(new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", i)));
        }

        // Act
        int processed = worker.processBatch(2);

        // Assert: stoppet ved maxJobs, 3 jobs venter stadig
        assertEquals(2, processed);
        assertEquals(3, queue.pendingJobs().size());
    }

    @Test
    void processBatchStopsWhenQueueEmptiesBeforeMaxJobs() {
        // Arrange: queue er bindende grænse
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        for (int i = 0; i < 2; i++) {
            queue.addToQueue(new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", i)));
        }

        // Act
        int processed = worker.processBatch(10);

        // Assert: queue tom før maxJobs ramt
        assertEquals(2, processed);
        assertEquals(0, queue.pendingJobs().size());
    }

    @Test
    void processBatchProcessesExactlyMaxWhenQueueSizeMatches() {
        // Arrange: match-boundary — begge stop-betingelser slår til samtidig
        InMemoryReplicationQueue queue = new InMemoryReplicationQueue();
        SecondaryReplicationGateway gateway = mock(SecondaryReplicationGateway.class);
        ReplicationWorker worker = new ReplicationWorker(queue, gateway, 3, 0);
        for (int i = 0; i < 3; i++) {
            queue.addToQueue(new ReplicationJob(ReplicationOperationType.CREATE, "account", Map.of("id", i)));
        }

        // Act
        int processed = worker.processBatch(3);

        // Assert: præcis maxJobs processed, queue tom
        assertEquals(3, processed);
        assertEquals(0, queue.pendingJobs().size());
    }

}
