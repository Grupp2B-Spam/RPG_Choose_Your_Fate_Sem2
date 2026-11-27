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
 * The schema is the file in db-init/, mounted exactly as compose.yaml mounts it, not
 * generated from the entities. With ddl-auto: validate, starting the context is itself the
 * check that the entities and the committed SQL agree.
 *
 * It also proves the boundary holds: this schema has to create on an empty database with no
 * reference to any other service's tables.
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
