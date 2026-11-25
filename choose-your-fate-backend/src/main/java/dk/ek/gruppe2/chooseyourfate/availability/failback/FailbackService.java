package dk.ek.gruppe2.chooseyourfate.availability.failback;

import dk.ek.gruppe2.chooseyourfate.availability.health.PrimaryHealthService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import org.springframework.stereotype.Service;

@Service
public class FailbackService {

    private final DatabaseRoutingService databaseRoutingService;
    private final PrimaryHealthService primaryHealthService;
    private final DataSynchronizationService dataSynchronizationService;

    public FailbackService(
            DatabaseRoutingService databaseRoutingService,
            PrimaryHealthService primaryHealthService,
            DataSynchronizationService dataSynchronizationService
    ) {
        this.databaseRoutingService = databaseRoutingService;
        this.primaryHealthService = primaryHealthService;
        this.dataSynchronizationService = dataSynchronizationService;
    }

    public void beginManualFailback() {
        if (!primaryHealthService.checkPrimaryHealth()) {
            throw new InvalidDatabaseStateTransitionException("Primary must be healthy before failback can begin");
        }
        databaseRoutingService.beginFailback();
    }

    public void completeManualFailback() {
        dataSynchronizationService.synchronizeSecondaryToPrimary();
        databaseRoutingService.completeFailback();
    }

    public void abortManualFailback() {
        databaseRoutingService.abortFailback();
    }
}
