package dk.ek.gruppe2.chooseyourfate.integration.availability;

import org.testcontainers.mysql.MySQLContainer;

public class AvailabilityTestContainerConfig {

    public static final MySQLContainer PRIMARY;
    public static final MySQLContainer SECONDARY;

    static {
        PRIMARY = new MySQLContainer("mysql:9.5")
                .withDatabaseName("choose_your_fate_test")
                .withUsername("root")
                .withPassword("root")
                .withInitScript("01_schema.sql")
                .withReuse(true);
        PRIMARY.start();

        SECONDARY = new MySQLContainer("mysql:9.5")
                .withDatabaseName("choose_your_fate_test")
                .withUsername("root")
                .withPassword("root")
                .withInitScript("01_schema.sql")
                .withReuse(true);
        SECONDARY.start();
    }
}
