package dk.ek.gruppe2.chooseyourfate.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationQueue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


// as it's an in memory queue its more a proof of concept as the queue is lost if the application is restarted.
// we should later store the queue in a database or make use of a real queue service like kafka.
@Service
public class InMemoryReplicationQueue implements ReplicationQueue {

    private final Queue<ReplicationJob> pending = new ConcurrentLinkedQueue<>();
    private final List<ReplicationJob> completed = new ArrayList<>();
    private final List<ReplicationJob> deadLetter = new ArrayList<>();

    @Override
    public void addToQueue(ReplicationJob job) {
        pending.add(job);
    }

    @Override
    public Optional<ReplicationJob> pollNext() {
        return Optional.ofNullable(pending.poll());
    }

    @Override
    public synchronized void markCompleted(ReplicationJob job) {
        if (completed.stream().noneMatch(completedJob -> completedJob.getId().equals(job.getId()))) {
            completed.add(job);
        }
    }

    @Override
    public void requeue(ReplicationJob job) {
        job.markPendingForRetry();
        pending.add(job);
    }

    @Override
    public synchronized void markDeadLetter(ReplicationJob job) {
        if (deadLetter.stream().noneMatch(deadLetterJob -> deadLetterJob.getId().equals(job.getId()))) {
            deadLetter.add(job);
        }
    }

    @Override
    public List<ReplicationJob> pendingJobs() {
        return List.copyOf(pending);
    }

    @Override
    public synchronized List<ReplicationJob> completedJobs() {
        return List.copyOf(completed);
    }

    @Override
    public synchronized List<ReplicationJob> deadLetterJobs() {
        return List.copyOf(deadLetter);
    }
}
