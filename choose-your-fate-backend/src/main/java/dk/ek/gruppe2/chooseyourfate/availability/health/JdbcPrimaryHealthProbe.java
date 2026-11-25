package dk.ek.gruppe2.chooseyourfate.availability.health;

import dk.ek.gruppe2.chooseyourfate.availability.routing.DatabaseRole;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcPrimaryHealthProbe implements SqlHealthProbe {

    private final JdbcTemplate jdbcTemplate;

    public JdbcPrimaryHealthProbe(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean isHealthy(DatabaseRole role) {
        if (role != DatabaseRole.PRIMARY) {
            return true;
        }

        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
