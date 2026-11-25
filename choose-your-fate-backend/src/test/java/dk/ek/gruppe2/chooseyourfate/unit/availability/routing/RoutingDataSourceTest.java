package dk.ek.gruppe2.chooseyourfate.unit.availability.routing;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRoutingService;
import dk.ek.gruppe2.chooseyourfate.availability.routing.RoutingDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
class RoutingDataSourceTest {

    @Test
    void lookupKeyIsPrimaryWhenRoutingServiceReportsPrimary() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        TestableRoutingDataSource routingDataSource = new TestableRoutingDataSource(routingService);

        // Act / Assert
        assertEquals(DatabaseRole.PRIMARY, routingDataSource.currentLookupKey());
    }

    @Test
    void lookupKeyFollowsRoleAfterFailover() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        TestableRoutingDataSource routingDataSource = new TestableRoutingDataSource(routingService);

        // Act
        routingService.beginFailover();
        routingService.completeFailover();

        // Assert
        assertEquals(DatabaseRole.SECONDARY, routingDataSource.currentLookupKey());
    }

    @Test
    void lookupKeyReturnsToPrimaryAfterFailback() {
        // Arrange
        DatabaseRoutingService routingService = new DatabaseRoutingService();
        routingService.beginFailover();
        routingService.completeFailover();
        routingService.beginFailback();
        routingService.completeFailback();
        TestableRoutingDataSource routingDataSource = new TestableRoutingDataSource(routingService);

        // Act / Assert
        assertEquals(DatabaseRole.PRIMARY, routingDataSource.currentLookupKey());
    }

    // Subclass that exposes the protected determineCurrentLookupKey() for testing
    private static class TestableRoutingDataSource extends RoutingDataSource {
        TestableRoutingDataSource(DatabaseRoutingService routingService) {
            super(routingService);
        }

        Object currentLookupKey() {
            return determineCurrentLookupKey();
        }
    }
}
