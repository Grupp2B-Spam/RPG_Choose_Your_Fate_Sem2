package dk.ek.gruppe2.chooseyourfate.availability.replication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
// get job from queue,
// tries to write the job to secondary
// marks the job as completed, retry or dead letter.
@Service
public class ReplicationWorker {
    // for getting and updating the pending jobs
    private final ReplicationQueue replicationQueue;
    // for writing the job to the secondary.
    private final SecondaryReplicationGateway secondaryReplicationGateway;
    // max amount of tries a job can fail before its dead.
    private final int maxRetries;
    private final long retryDelayMillis;
    private final int scheduledBatchSize = 10;

    public ReplicationWorker(
            ReplicationQueue replicationQueue,
            SecondaryReplicationGateway secondaryReplicationGateway,
            @Value("${app.availability.replication.max-retries:3}") int maxRetries,
            @Value("${app.availability.replication.retry-delay-ms:5000}") long retryDelayMillis
    ) {
        this.replicationQueue = replicationQueue;
        this.secondaryReplicationGateway = secondaryReplicationGateway;
        this.maxRetries = Math.max(1, maxRetries);
        this.retryDelayMillis = Math.max(0, retryDelayMillis);
    }

    @Scheduled(fixedDelayString = "${app.availability.replication.worker-interval-ms:2000}")
    public void processPendingJobs() {
        processBatch(scheduledBatchSize);
    }

    @Async
    @EventListener
    public void processPendingJobsWhenJobIsQueued(ReplicationJobQueuedEvent ignored) {
        processBatch(scheduledBatchSize);
    }

    // get the next job from the queue and writes it to the secondary if it can.
    // return ture if the job got successfully written to the secondary.
    // returns false if the queue is empty.
    public boolean processNext() {
        return replicationQueue.pollNext()
                .map(this::process)
                .orElse(false);
    }

    // Used to handle up to maxJobs at the time, and stop if queue becomes empty.
    // returns have many jobs was treated.
    public int processBatch(int maxJobs) {
        int processed = 0;
        while (processed < maxJobs && processNext()) {
            processed++;
        }
        return processed;
    }
    // Happy path: Jobs marked as in progress, gateway writes the job to secondary, if successfully marks job as completed, job is moved to the completedList
    // unHappe path: job marked as failed, retry count incremented, if retry limit is reached the job is moved to dead letter otherwise it goes back in the queue.
    private boolean process(ReplicationJob job) {
        if (!job.isReadyForAttempt(java.time.Instant.now())) {
            replicationQueue.requeue(job);
            return false;
        }

        job.markInProgress();
        try {
            secondaryReplicationGateway.apply(job);
            job.markCompleted();
            replicationQueue.markCompleted(job);
        } catch (RuntimeException ex) {
            job.markFailed(ex, maxRetries, retryDelayMillis);
            if (job.getStatus() == ReplicationStatus.DEAD_LETTER) {
                replicationQueue.markDeadLetter(job);
            } else {
                replicationQueue.requeue(job);
            }
        }
        return true;
    }
}
