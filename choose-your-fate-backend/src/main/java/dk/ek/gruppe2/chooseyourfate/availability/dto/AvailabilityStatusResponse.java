package dk.ek.gruppe2.chooseyourfate.availability.dto;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;

//Our observability output
public record AvailabilityStatusResponse(
        DatabaseSystemState state,
        DatabaseRole activeRole,
        boolean maintenanceMode,
        int primaryConsecutiveFailures,
        int pendingReplicationJobs,
        int completedReplicationJobs,
        int deadLetterReplicationJobs
) {
}
