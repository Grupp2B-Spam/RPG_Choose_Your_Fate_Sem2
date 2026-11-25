package dk.ek.gruppe2.chooseyourfate.availability.replication;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


// ReplicationJob is the unit of work for asynchronous replication. It records what operation happened on primary, what data should be applied to secondary, and tracks retry/status information so replication failures are observable and testable.
public class ReplicationJob {

    private final UUID id;
    private final ReplicationOperationType operationType;
    private final String entityName;
    private final Map<String, Object> payload;
    private final Instant createdAt;
    private Instant lastAttemptAt;
    private Instant nextAttemptAt;
    private ReplicationStatus status;
    private int retryCount;
    private String lastError;

    public ReplicationJob(
            ReplicationOperationType operationType,
            String entityName,
            Map<String, Object> payload
    ) {
        this.id = UUID.randomUUID();
        this.operationType = operationType;
        this.entityName = entityName;
        this.payload = new LinkedHashMap<>(payload);
        this.createdAt = Instant.now();
        this.status = ReplicationStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public ReplicationOperationType getOperationType() {
        return operationType;
    }

    public String getEntityName() {
        return entityName;
    }

    public Map<String, Object> getPayload() {
        return Map.copyOf(payload);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    void markInProgress() {
        status = ReplicationStatus.IN_PROGRESS;
        lastAttemptAt = Instant.now();
    }

    void markCompleted() {
        status = ReplicationStatus.COMPLETED;
        lastError = null;
        nextAttemptAt = null;
    }

    void markFailed(RuntimeException ex, int maxRetries, long retryDelayMillis) {
        retryCount++;
        lastError = ex.getMessage();
        status = retryCount >= maxRetries ? ReplicationStatus.DEAD_LETTER : ReplicationStatus.FAILED;
        nextAttemptAt = status == ReplicationStatus.DEAD_LETTER
                ? null
                : Instant.now().plusMillis(Math.max(0, retryDelayMillis));
    }

    void markPendingForRetry() {
        status = ReplicationStatus.PENDING;
    }

    boolean isReadyForAttempt(Instant now) {
        return nextAttemptAt == null || !nextAttemptAt.isAfter(now);
    }
}
