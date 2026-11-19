package dk.ek.gruppe2.chooseyourfate;

import org.testcontainers.mysql.MySQLContainer;

public class TestContainerConfig {

    public static final MySQLContainer MYSQL;

    static {
        MYSQL = new MySQLContainer("mysql:9.5")
                .withDatabaseName("choose_your_fate_test")
                .withUsername("root")
                .withPassword("root")
                .withInitScript("01_schema.sql")
                .withReuse(true);
        MYSQL.start();
    }
}
