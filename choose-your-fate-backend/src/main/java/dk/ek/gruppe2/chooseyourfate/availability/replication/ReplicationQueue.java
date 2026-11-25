package dk.ek.gruppe2.chooseyourfate.availability.replication;

import java.util.List;
import java.util.Optional;


// we only have the queue in memory for now.
public interface ReplicationQueue {

    void addToQueue(ReplicationJob job);

    Optional<ReplicationJob> pollNext();

    void markCompleted(ReplicationJob job);

    void requeue(ReplicationJob job);

    void markDeadLetter(ReplicationJob job);

    List<ReplicationJob> pendingJobs();

    List<ReplicationJob> completedJobs();

    List<ReplicationJob> deadLetterJobs();
}
