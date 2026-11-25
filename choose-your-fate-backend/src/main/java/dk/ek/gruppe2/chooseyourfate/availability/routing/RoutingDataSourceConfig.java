package dk.ek.gruppe2.chooseyourfate.availability.routing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class RoutingDataSourceConfig {

    @Bean(name = "routingDataSource")
    @Primary
    public DataSource routingDataSource(
            DatabaseRoutingService routingService,
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("secondaryDataSource") DataSource secondaryDataSource
    ) {
        RoutingDataSource routing = new RoutingDataSource(routingService);
        routing.setTargetDataSources(Map.of(
                DatabaseRole.PRIMARY, primaryDataSource,
                DatabaseRole.SECONDARY, secondaryDataSource
        ));
        routing.setDefaultTargetDataSource(primaryDataSource);
        routing.afterPropertiesSet();
        return routing;
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new JdbcTemplate(routingDataSource);
    }
}
