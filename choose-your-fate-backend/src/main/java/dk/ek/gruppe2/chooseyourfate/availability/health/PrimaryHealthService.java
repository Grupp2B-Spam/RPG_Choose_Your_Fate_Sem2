package dk.ek.gruppe2.chooseyourfate.availability.health;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


//this service judges if primary is still healthy or had failed enough
//for a failover is valid.
@Service
public class PrimaryHealthService {

    private final SqlHealthProbe sqlHealthProbe;
    private final int failureThreshold;
    private int consecutiveFailures;

    public PrimaryHealthService(
            SqlHealthProbe sqlHealthProbe,
            @Value("${app.availability.primary-failure-threshold:2}") int failureThreshold
    ) {
        this.sqlHealthProbe = sqlHealthProbe;
        this.failureThreshold = Math.max(1, failureThreshold);
    }

    public synchronized boolean checkPrimaryHealth() {
        boolean healthy = sqlHealthProbe.isHealthy(DatabaseRole.PRIMARY);
        if (healthy) {
            consecutiveFailures = 0;
        } else {
            consecutiveFailures++;
        }
        return healthy;
    }

    public synchronized boolean isPrimaryUnavailable() {
        return consecutiveFailures >= failureThreshold;
    }

    public synchronized int consecutiveFailures() {
        return consecutiveFailures;
    }
}
