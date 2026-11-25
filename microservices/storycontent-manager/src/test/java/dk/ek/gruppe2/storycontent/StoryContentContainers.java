package dk.ek.gruppe2.storycontent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * The two containers the integration tests run against.
 *
 * <p>The database image and the schema are deliberately the real ones. The schema is the
 * file in {@code db-init/}, copied to {@code /docker-entrypoint-initdb.d} exactly as
 * {@code compose.yaml} mounts it, rather than being generated from the entities by
 * Hibernate. That matters for two reasons:
 *
 * <ul>
 *   <li>It tests the file the database is actually created from. A schema generated from
 *       the entities would pass whatever the entities say, including things the committed
 *       SQL does not contain.</li>
 *   <li>With {@code ddl-auto: validate}, starting the context is itself the check that the
 *       entities and the committed SQL agree. A column added to one and forgotten in the
 *       other fails the build here instead of appearing in the cluster.</li>
 * </ul>
 *
 * <p>It also proves the boundary holds: this schema has to create on an empty database
 * with no reference to any other service's tables. A foreign key left dangling by the
 * split would show up here.
 */
@TestConfiguration(proxyBeanMethods = false)
public class StoryContentContainers {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    MySQLContainer storyContentVault() {
        return new MySQLContainer(DockerImageName.parse("mysql:9.5.0"))
                .withDatabaseName("story_content")
                .withUsername("root")
                .withPassword("test")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("db-init/01_schema.sql"),
                        "/docker-entrypoint-initdb.d/01_schema.sql");
    }

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    RabbitMQContainer broker() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
    }
}
