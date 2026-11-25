package dk.ek.gruppe2.chooseyourfate.integration.availability;

import dk.ek.gruppe2.chooseyourfate.availability.failback.FailbackService;
import dk.ek.gruppe2.chooseyourfate.availability.failover.FailoverService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;
import dk.ek.gruppe2.chooseyourfate.dto.CreateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest
public class AccountFailoverFlowIT {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Primary connection
        registry.add("spring.datasource.url",
                AvailabilityTestContainerConfig.PRIMARY::getJdbcUrl);
        registry.add("spring.datasource.username",
                AvailabilityTestContainerConfig.PRIMARY::getUsername);
        registry.add("spring.datasource.password",
                AvailabilityTestContainerConfig.PRIMARY::getPassword);

        // Secondary connection
        registry.add("app.datasource.secondary.url",
                AvailabilityTestContainerConfig.SECONDARY::getJdbcUrl);
        registry.add("app.datasource.secondary.username",
                AvailabilityTestContainerConfig.SECONDARY::getUsername);
        registry.add("app.datasource.secondary.password",
                AvailabilityTestContainerConfig.SECONDARY::getPassword);
    }

    @Autowired AccountService accountService;
    @Autowired FailoverService failoverService;
    @Autowired FailbackService failbackService;
    @Autowired DatabaseRoutingService routingService;

    @Autowired @Qualifier("primaryJdbcTemplate") JdbcTemplate primaryJdbcTemplate;
    @Autowired @Qualifier("secondaryJdbcTemplate") JdbcTemplate secondaryJdbcTemplate;

    @Test
    void writesPhysicallyChangeDatabaseAfterFailover() {
        // Unique suffix per run — containers are reused, so we avoid collisions
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String beforeUsername = "before_" + runId;
        String afterUsername = "after_" + runId;

        // Phase 1: PRIMARY_ACTIVE — write hits primary, replicates to secondary
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state(),
                "Pre-condition: system must start in PRIMARY_ACTIVE");

        accountService.createAccount(buildCreateRequest(beforeUsername));

        // Write should be in primary immediately (synchronous)
        assertTrue(accountExistsIn(primaryJdbcTemplate, beforeUsername),
                "Account written before failover should exist in primary");

        // Replication is async — poll secondary for up to 10 seconds
        waitForAccountIn(secondaryJdbcTemplate, beforeUsername, Duration.ofSeconds(10));

        // Phase 2: Trigger failover — state flips to SECONDARY_ACTIVE
        failoverService.triggerManualFailover();
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state(),
                "Failover must transition to SECONDARY_ACTIVE");

        // Phase 3: Write after failover — must land in SECONDARY only
        accountService.createAccount(buildCreateRequest(afterUsername));

        assertTrue(accountExistsIn(secondaryJdbcTemplate, afterUsername),
                "Account written after failover should exist in secondary");
        assertFalse(accountExistsIn(primaryJdbcTemplate, afterUsername),
                "Account written after failover must NOT exist in primary — this is the proof that routing works");

        // Phase 4: Failback to leave context clean for other tests
        failbackService.beginManualFailback();
        failbackService.completeManualFailback();
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state(),
                "After failback, state must be PRIMARY_ACTIVE");
    }

    // ---------- Helpers ----------

    private CreateAccountRequestDTO buildCreateRequest(String username) {
        CreateAccountRequestDTO request = new CreateAccountRequestDTO();
        request.setUsername(username);
        request.setEmail(username + "@test.dk");
        request.setPassword("password123");
        return request;
    }

    private boolean accountExistsIn(JdbcTemplate jdbc, String username) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM account WHERE username = ?",
                Integer.class, username);
        return count != null && count > 0;
    }

    private void waitForAccountIn(JdbcTemplate jdbc, String username, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (accountExistsIn(jdbc, username)) return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("Timed out waiting for username '" + username + "' to replicate");
    }
}
