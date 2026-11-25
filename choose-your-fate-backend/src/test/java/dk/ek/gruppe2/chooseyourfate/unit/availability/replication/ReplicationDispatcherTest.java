package dk.ek.gruppe2.chooseyourfate.unit.availability.replication;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationJob;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationOperationType;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationDispatcher;
import dk.ek.gruppe2.chooseyourfate.availability.replication.replicators.EntityReplicator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class ReplicationDispatcherTest {

    @Test
    void createJobIsDispatchedToReplicatorMatchingEntityName() {
        // Arrange
        EntityReplicator account = mock(EntityReplicator.class);
        when(account.entityName()).thenReturn("account");
        ReplicationDispatcher gateway =
                new ReplicationDispatcher(List.of(account));

        // Act
        gateway.apply(new ReplicationJob(
                ReplicationOperationType.CREATE, "account", Map.of("id", 1)));

        // Assert
        verify(account).create(Map.of("id", 1));
        verify(account, never()).update(anyMap());
        verify(account, never()).delete(anyMap());
    }

    @Test
    void updateJobIsDispatchedToReplicatorMatchingEntityName() {
        // Arrange
        EntityReplicator account = mock(EntityReplicator.class);
        when(account.entityName()).thenReturn("account");
        ReplicationDispatcher gateway =
                new ReplicationDispatcher(List.of(account));

        // Act
        gateway.apply(new ReplicationJob(
                ReplicationOperationType.UPDATE, "account", Map.of("id", 1)));

        // Assert
        verify(account).update(Map.of("id", 1));
        verify(account, never()).create(anyMap());
        verify(account, never()).delete(anyMap());
    }

    @Test
    void deleteJobIsDispatchedToReplicatorMatchingEntityName() {
        // Arrange
        EntityReplicator account = mock(EntityReplicator.class);
        when(account.entityName()).thenReturn("account");
        ReplicationDispatcher gateway =
                new ReplicationDispatcher(List.of(account));

        // Act
        gateway.apply(new ReplicationJob(
                ReplicationOperationType.DELETE, "account", Map.of("id", 1)));

        // Assert
        verify(account).delete(Map.of("id", 1));
        verify(account, never()).create(anyMap());
        verify(account, never()).update(anyMap());
    }

    @Test
    void jobForUnregisteredEntityIsSilentlyIgnored() {
        // Arrange — only an account replicator is registered
        EntityReplicator account = mock(EntityReplicator.class);
        when(account.entityName()).thenReturn("account");
        ReplicationDispatcher gateway =
                new ReplicationDispatcher(List.of(account));

        // Act — but the job is for a different entity
        gateway.apply(new ReplicationJob(
                ReplicationOperationType.CREATE, "character", Map.of("id", 1)));

        // Assert — no replicator was called
        verify(account, never()).create(anyMap());
        verify(account, never()).update(anyMap());
        verify(account, never()).delete(anyMap());
    }
}
