package dk.ek.gruppe2.chooseyourfate.availability.failback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ManualDataSynchronizationService implements DataSynchronizationService {

    private static final Logger log = LoggerFactory.getLogger(ManualDataSynchronizationService.class);

    @Override
    public void synchronizeSecondaryToPrimary() {
        log.warn("=== MANUAL SECONDARY-TO-PRIMARY SYNC REQUIRED ===");
        log.warn("Automatic sync is not implemented. In production, a DBA must run");
        log.warn("reviewed SQL sync scripts BEFORE completing failback. Failback is");
        log.warn("proceeding without sync — data written to secondary during the");
        log.warn("failover window may be lost on the next primary write.");
    }
}
