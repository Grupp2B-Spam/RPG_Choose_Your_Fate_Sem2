package dk.ek.gruppe2.chooseyourfate.unit.availability.routing;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseSystemState;
import dk.ek.gruppe2.chooseyourfate.availability.routing.InvalidDatabaseStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseRoutingServiceTest {

    // VALID PARTITIONS

    @Test
    void startsWithPrimaryActiveForReadsAndWrites() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();

        // Act / Assert
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.PRIMARY, routingService.routeRead());
        assertEquals(DatabaseRole.PRIMARY, routingService.routeWrite());
        assertFalse(routingService.isMaintenanceMode());
    }

    @Test
    void failoverSwitchesActiveRoleToSecondary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();

        // Act
        routingService.beginFailover();

        // Assert
        assertEquals(DatabaseSystemState.FAILOVER_IN_PROGRESS, routingService.state());

        // Act
        routingService.completeFailover();

        // Assert
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.SECONDARY, routingService.routeRead());
        assertEquals(DatabaseRole.SECONDARY, routingService.routeWrite());
    }

    @Test
    void failoverCanBeAbortedWhileInProgress() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();

        // Act
        routingService.beginFailover();
        routingService.abortFailover();

        // Assert
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.PRIMARY, routingService.activeRole());
    }

    @Test
    void failbackUsesMaintenanceModeAndReturnsToPrimary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();

        // Act
        routingService.beginFailback();

        // Assert
        assertEquals(DatabaseSystemState.FAILBACK_IN_PROGRESS, routingService.state());
        assertTrue(routingService.isMaintenanceMode());

        // Act
        routingService.completeFailback();

        // Assert
        assertEquals(DatabaseSystemState.PRIMARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.PRIMARY, routingService.activeRole());
        assertFalse(routingService.isMaintenanceMode());
    }

    @Test
    void failbackCanBeAbortedWhileInProgress() {
        // Arrange — navigate to FAILBACK_IN_PROGRESS
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();
        routingService.beginFailback();

        // Act
        routingService.abortFailback();

        // Assert: returns to SECONDARY_ACTIVE, maintenance mode off
        assertEquals(DatabaseSystemState.SECONDARY_ACTIVE, routingService.state());
        assertEquals(DatabaseRole.SECONDARY, routingService.activeRole());
        assertFalse(routingService.isMaintenanceMode());
    }


    // INVALID PARTITIONS

    @ParameterizedTest(name = "{0}: {1} should be rejected")
    @MethodSource("invalidStateTransitions")
    void invalidStateTransitionIsRejected(
            String fromState,
            String operationName,
            Consumer<DatabaseRoutingService> setupState,
            Consumer<DatabaseRoutingService> invalidOperation
    ) {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        setupState.accept(routingService);
        DatabaseSystemState stateBefore = routingService.state();
        DatabaseRole roleBefore = routingService.activeRole();

        // Act / Assert
        assertThrows(InvalidDatabaseStateTransitionException.class,
                () -> invalidOperation.accept(routingService));

        // Assert: state and role unchanged
        assertEquals(stateBefore, routingService.state(),
                "State must not change when invalid transition is rejected");
        assertEquals(roleBefore, routingService.activeRole(),
                "Active role must not change when invalid transition is rejected");
    }

    private static Stream<Arguments> invalidStateTransitions() {
        // State setup consumers
        Consumer<DatabaseRoutingService> stayPrimary = r -> {};
        Consumer<DatabaseRoutingService> toFailoverInProgress = DatabaseRoutingService::beginFailover;
        Consumer<DatabaseRoutingService> toSecondaryActive = r -> {
            r.beginFailover();
            r.completeFailover();
        };
        Consumer<DatabaseRoutingService> toFailbackInProgress = r -> {
            r.beginFailover();
            r.completeFailover();
            r.beginFailback();
        };

        // Operation consumers
        Consumer<DatabaseRoutingService> beginFailover = DatabaseRoutingService::beginFailover;
        Consumer<DatabaseRoutingService> completeFailover = DatabaseRoutingService::completeFailover;
        Consumer<DatabaseRoutingService> abortFailover = DatabaseRoutingService::abortFailover;
        Consumer<DatabaseRoutingService> beginFailback = DatabaseRoutingService::beginFailback;
        Consumer<DatabaseRoutingService> completeFailback = DatabaseRoutingService::completeFailback;
        Consumer<DatabaseRoutingService> abortFailback = DatabaseRoutingService::abortFailback;

        return Stream.of(
                // From PRIMARY_ACTIVE (only beginFailover is valid)
                Arguments.of("PRIMARY_ACTIVE", "completeFailover", stayPrimary, completeFailover),
                Arguments.of("PRIMARY_ACTIVE", "abortFailover",    stayPrimary, abortFailover),
                Arguments.of("PRIMARY_ACTIVE", "beginFailback",    stayPrimary, beginFailback),
                Arguments.of("PRIMARY_ACTIVE", "completeFailback", stayPrimary, completeFailback),
                Arguments.of("PRIMARY_ACTIVE", "abortFailback",    stayPrimary, abortFailback),

                // From FAILOVER_IN_PROGRESS (only completeFailover, abortFailover are valid)
                Arguments.of("FAILOVER_IN_PROGRESS", "beginFailover",    toFailoverInProgress, beginFailover),
                Arguments.of("FAILOVER_IN_PROGRESS", "beginFailback",    toFailoverInProgress, beginFailback),
                Arguments.of("FAILOVER_IN_PROGRESS", "completeFailback", toFailoverInProgress, completeFailback),
                Arguments.of("FAILOVER_IN_PROGRESS", "abortFailback",    toFailoverInProgress, abortFailback),

                // From SECONDARY_ACTIVE (only beginFailback is valid)
                Arguments.of("SECONDARY_ACTIVE", "beginFailover",    toSecondaryActive, beginFailover),
                Arguments.of("SECONDARY_ACTIVE", "completeFailover", toSecondaryActive, completeFailover),
                Arguments.of("SECONDARY_ACTIVE", "abortFailover",    toSecondaryActive, abortFailover),
                Arguments.of("SECONDARY_ACTIVE", "completeFailback", toSecondaryActive, completeFailback),
                Arguments.of("SECONDARY_ACTIVE", "abortFailback",    toSecondaryActive, abortFailback),

                // From FAILBACK_IN_PROGRESS (only completeFailback, abortFailback are valid)
                Arguments.of("FAILBACK_IN_PROGRESS", "beginFailover",    toFailbackInProgress, beginFailover),
                Arguments.of("FAILBACK_IN_PROGRESS", "completeFailover", toFailbackInProgress, completeFailover),
                Arguments.of("FAILBACK_IN_PROGRESS", "abortFailover",    toFailbackInProgress, abortFailover),
                Arguments.of("FAILBACK_IN_PROGRESS", "beginFailback",    toFailbackInProgress, beginFailback)
        );
    }
}