package dk.ek.gruppe2.chooseyourfate.unit.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.WriteGate;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteGateTest {

    private static Stream<Arguments> blockedStates() {
        return Stream.of(
                Arguments.of(
                        DatabaseSystemState.FAILOVER_IN_PROGRESS,
                        (Consumer<DatabaseRoutingService>) DatabaseRoutingService::beginFailover
                ),
                Arguments.of(
                        DatabaseSystemState.FAILBACK_IN_PROGRESS,
                        (Consumer<DatabaseRoutingService>) routingService -> {
                            routingService.beginFailover();
                            routingService.completeFailover();
                            routingService.beginFailback();
                        }
                )
        );
    }

    // ---------- Valid partitions (write allowed) ----------

    @Test
    void writeOnPrimaryActiveShouldBePrimary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        WriteGate writeGate = new WriteGate(routingService);

        // Act
        DatabaseRole target = writeGate.getTargetForWrite();

        // Assert
        assertEquals(DatabaseRole.PRIMARY, target);
    }

    @Test
    void writeOnSecondaryActiveShouldBeSecondary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();
        WriteGate writeGate = new WriteGate(routingService);

        // Act
        DatabaseRole target = writeGate.getTargetForWrite();

        // Assert
        assertEquals(DatabaseRole.SECONDARY, target);
    }

    // ---------- Invalid partitions (write blocked) ----------

    @ParameterizedTest
    @MethodSource("blockedStates")
    void writeIsBlockedDuringTransitionStates(DatabaseSystemState expectedState, Consumer<DatabaseRoutingService> stateSetup) {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        stateSetup.accept(routingService);
        WriteGate writeGate = new WriteGate(routingService);

        // Act / Assert
        InvalidDatabaseStateTransitionException ex = assertThrows(
                InvalidDatabaseStateTransitionException.class,
                writeGate::getTargetForWrite
        );

        // Assert
        assertEquals("Writes are blocked while state is " + expectedState, ex.getMessage());
    }
}