package dk.ek.gruppe2.chooseyourfate.availability.replication;

public interface SecondaryReplicationGateway {

    void apply(ReplicationJob job);
}
