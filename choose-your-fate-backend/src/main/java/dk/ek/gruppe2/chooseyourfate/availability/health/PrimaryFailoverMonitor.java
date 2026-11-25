package dk.ek.gruppe2.chooseyourfate.availability.health;

import dk.ek.gruppe2.chooseyourfate.availability.failover.FailoverService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


// asks FailoverService to valuate if a failover is needed.
@Service
public class PrimaryFailoverMonitor {

    private final FailoverService failoverService;

    public PrimaryFailoverMonitor(FailoverService failoverService) {
        this.failoverService = failoverService;
    }

    @Scheduled(fixedDelayString = "${app.availability.health-check-interval-ms:5000}")
    public void monitorPrimary() {
        failoverService.evaluateAndFailoverIfNeeded();
    }
}
