package dk.ek.gruppe2.chooseyourfate.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import org.springframework.stereotype.Service;

@Service
public class WriteGate {

    private final DatabaseRoutingService databaseRoutingService;

    public WriteGate(DatabaseRoutingService databaseRoutingService) {
        this.databaseRoutingService = databaseRoutingService;
    }
    /**
     * Call before performing a write. Throws if writes are currently blocked
     * (state in transition). Returns the database the write will physically hit,
     * so the caller knows whether to emit a replication event.
     */
    public DatabaseRole getTargetForWrite(){
        DatabaseSystemState state = databaseRoutingService.state();
        if(state == DatabaseSystemState.FAILBACK_IN_PROGRESS || state == DatabaseSystemState.FAILOVER_IN_PROGRESS){
            throw new InvalidDatabaseStateTransitionException("Writes are blocked while state is " + state);
        }
        return databaseRoutingService.routeWrite();
    }
}
