package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.availability.dto.AvailabilityStatusResponse;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.failback.FailbackService;
import dk.ek.gruppe2.chooseyourfate.availability.failover.FailoverService;
import dk.ek.gruppe2.chooseyourfate.availability.health.PrimaryHealthService;
import dk.ek.gruppe2.chooseyourfate.availability.replication.ReplicationQueue;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final DatabaseRoutingService databaseRoutingService;
    private final PrimaryHealthService primaryHealthService;
    private final ReplicationQueue replicationQueue;
    private final FailoverService failoverService;
    private final FailbackService failbackService;

    public AvailabilityController(
            DatabaseRoutingService databaseRoutingService,
            PrimaryHealthService primaryHealthService,
            ReplicationQueue replicationQueue,
            FailoverService failoverService,
            FailbackService failbackService
    ) {
        this.databaseRoutingService = databaseRoutingService;
        this.primaryHealthService = primaryHealthService;
        this.replicationQueue = replicationQueue;
        this.failoverService = failoverService;
        this.failbackService = failbackService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public AvailabilityStatusResponse status() {
        return new AvailabilityStatusResponse(
                databaseRoutingService.state(),
                databaseRoutingService.activeRole(),
                databaseRoutingService.isMaintenanceMode(),
                primaryHealthService.consecutiveFailures(),
                replicationQueue.pendingJobs().size(),
                replicationQueue.completedJobs().size(),
                replicationQueue.deadLetterJobs().size()
        );
    }

    @PostMapping("/failover")
    @PreAuthorize("hasRole('ADMIN')")
    public AvailabilityStatusResponse failover() {
        failoverService.triggerManualFailover();
        return status();
    }

    @PostMapping("/failback/begin")
    @PreAuthorize("hasRole('ADMIN')")
    public AvailabilityStatusResponse beginFailback() {
        failbackService.beginManualFailback();
        return status();
    }

    @PostMapping("/failback/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public AvailabilityStatusResponse completeFailback() {
        failbackService.completeManualFailback();
        return status();
    }
}
