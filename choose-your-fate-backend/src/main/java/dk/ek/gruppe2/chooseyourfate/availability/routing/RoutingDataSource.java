package dk.ek.gruppe2.chooseyourfate.availability.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

// AbstractRoutingDataSource comes from spring-jdbc
public class RoutingDataSource extends AbstractRoutingDataSource {

    private final DatabaseRoutingService routingService;
    public RoutingDataSource(DatabaseRoutingService routingService) {
        this.routingService = routingService;
    }

    // gets called each time spring asks DataSource for a new connection.
    @Override
    protected Object determineCurrentLookupKey() {
        return routingService.activeRole();
    }

}
