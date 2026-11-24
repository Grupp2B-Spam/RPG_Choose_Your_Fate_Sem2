package dk.ek.gruppe2.recapmanager.integration;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.exception.RecapConflictException;
import dk.ek.gruppe2.recapmanager.messaging.RecapEventOutbox;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.*;
import dk.ek.gruppe2.recapmanager.service.CharacterPathService;
import dk.ek.gruppe2.recapmanager.speech.SpeechContent;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@SpringJUnitConfig(RecapOutboxTransactionIT.Config.class)
class RecapOutboxTransactionIT {
    @Autowired CharacterPathService service;
    @Autowired CharacterPathRepository paths;
    @Autowired RecapOutboxRepository outbox;
    @Autowired PlatformTransactionManager transactions;
    @Autowired ObjectMapper mapper;
    @Autowired SpeechProperties speech;

    @BeforeEach void reset() {
        outbox.deleteAll();
        paths.deleteAll();
    }

    @Test void summaryAndPublicationIntentCommitTogether() {
        var saved = service.saveUpdatedSummary(SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY), "A journey", 2);
        assertThat(paths.count()).isEqualTo(1);
        var message = outbox.findAll().getFirst();
        var event = mapper.readTree(message.getPayload());
        assertThat(event.path("eventId").asString()).isEqualTo(message.getId());
        assertThat(event.path("eventType").asString()).isEqualTo("RecapGenerated");
        assertThat(event.path("requestType").asString()).isEqualTo("PATH_SUMMARY");
        assertThat(event.path("throughSequence").asLong()).isEqualTo(2);
        assertThat(event.path("text").asString()).isEqualTo(saved.summary());
        assertThat(event.path("contentHash").asString())
                .isEqualTo(SpeechContent.request(saved.summary(), speech).getContentHash());
    }

    @Test void rolledBackSummaryCannotLeaveAnEventToPublish() {
        var transaction = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> transaction.execute(status -> {
            service.saveUpdatedSummary(SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY), "A journey", 1);
            throw new IllegalStateException("roll back");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(paths.count()).isZero();
        assertThat(outbox.count()).isZero();
    }

    @Test void characterRecapDoesNotPublishSpeechWork() {
        service.saveUpdatedSummary(SummarySnapshot.empty(1, AiRequestType.CHARACTER_RECAP), "You travelled", 1);
        assertThat(paths.count()).isEqualTo(1);
        assertThat(outbox.count()).isZero();
    }

    @Test void duplicateCreationCannotCreateAnExtraEvent() {
        var empty = SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY);
        service.saveUpdatedSummary(empty, "Winning journey", 1);
        assertThatThrownBy(() -> service.saveUpdatedSummary(empty, "Losing journey", 1))
                .isInstanceOf(RecapConflictException.class);
        assertThat(outbox.count()).isEqualTo(1);
        assertThat(service.load(1, AiRequestType.PATH_SUMMARY).summary()).isEqualTo("Winning journey");
    }

    @Test void staleUpdateCannotReplaceANewerNarrativeOrPublishAnotherEvent() {
        var original = service.saveUpdatedSummary(SummarySnapshot.empty(1, AiRequestType.PATH_SUMMARY), "Original journey", 1);
        var winner = service.saveUpdatedSummary(original, "Winning update", 2);
        assertThat(winner.version()).isGreaterThan(original.version());

        assertThatThrownBy(() -> service.saveUpdatedSummary(original, "Stale update", 3))
                .isInstanceOf(RecapConflictException.class);

        assertThat(service.load(1, AiRequestType.PATH_SUMMARY)).isEqualTo(winner);
        assertThat(outbox.count()).isEqualTo(2);
        assertThat(outbox.findAll()).noneMatch(message -> message.getPayload().contains("Stale update"));
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = CharacterPathRepository.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:recap_outbox_" + java.util.UUID.randomUUID()
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("dk.ek.gruppe2.recapmanager.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ObjectMapper mapper() { return JsonMapper.builder().build(); }
        @Bean SpeechProperties speech() { return new SpeechProperties(); }
        @Bean RecapEventOutbox eventOutbox(RecapOutboxRepository repository, SpeechProperties speech, ObjectMapper mapper) {
            return new RecapEventOutbox(repository, speech, mapper);
        }
        @Bean CharacterPathService paths(CharacterPathRepository repository, RecapEventOutbox outbox) {
            return new CharacterPathService(repository, outbox);
        }
    }
}
