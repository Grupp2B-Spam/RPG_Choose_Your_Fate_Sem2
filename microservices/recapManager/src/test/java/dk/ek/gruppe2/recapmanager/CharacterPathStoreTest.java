package dk.ek.gruppe2.recapmanager;

import dk.ek.gruppe2.recapmanager.exception.RecapConflictException;
import dk.ek.gruppe2.recapmanager.model.CharacterPath;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.CharacterPathRepository;
import dk.ek.gruppe2.recapmanager.service.CharacterPathService;
import dk.ek.gruppe2.recapmanager.service.AiService;
import dk.ek.gruppe2.recapmanager.ai.AiTools;
import dk.ek.gruppe2.recapmanager.config.AiDeploymentProperties;
import org.springframework.ai.chat.client.ChatClient;
import dk.ek.gruppe2.recapmanager.config.StoryPathProperties;
import dk.ek.gruppe2.recapmanager.dto.AiRequestDTO;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.storypath.*;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static dk.ek.gruppe2.recapmanager.enums.AiRequestType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(CharacterPathStoreTest.DatabaseConfig.class)
class CharacterPathStoreTest {
    @Autowired CharacterPathRepository repository;
    @Autowired CharacterPathService store;

    @BeforeEach void resetDatabase() { repository.deleteAll(); }

    @Test
    void newSummaryAndCheckpointArePersistedTogether() {
        var created = store.saveUpdatedSummary(SummarySnapshot.empty(42, PATH_SUMMARY), "New summary", 2);
        assertThat(created.id()).isNotNull();
        assertThat(created.version()).isZero();
        assertThat(store.load(42, PATH_SUMMARY)).isEqualTo(created);
    }

    @Test
    void repeatedRequestReusesStoredTextUntilANewSourceEventAppears() {
        var history = mock(JourneyHistoryClient.class);
        var model = new ToolCallingTestModel();
        var authorization = mock(CharacterAuthorizationService.class);
        var tools = new AiTools(store, history, authorization);
        var service = new AiService(ChatClient.builder(model), tools, new AiDeploymentProperties(), new StoryPathProperties());
        var entries = new ArrayList<JourneyChoice>();
        var first = new JourneyChoice(1, 10, "Village", "Road", "Left the village", "", 5);
        var second = new JourneyChoice(2, 11, "Road", "Bridge", "Helped a traveler", "An ally joined", 8);
        entries.add(first);
        when(history.fetchAfter(eq(42), anyLong())).thenAnswer(call -> {
            long cursor = call.getArgument(1);
            return entries.stream().filter(choice -> choice.sequence() > cursor).toList();
        });
        model.behavior = callbacks -> callbacks.generate(model.calls == 1 ? "First summary" : "Combined summary");
        var request = new AiRequestDTO();
        request.setCharacterId(42);
        request.setRequestType(PATH_SUMMARY);

        assertThat(service.handleRequest(request)).isEqualTo("First summary");
        var saved = repository.findByCharacterIdAndRequestType(42, PATH_SUMMARY);
        var savedTimestamp = saved.getSummaryUpdatedAt();
        var savedVersion = saved.getVersion();
        assertThat(service.handleRequest(request)).isEqualTo("First summary");
        var unchanged = repository.findById(saved.getId()).orElseThrow();
        assertThat(unchanged.getVersion()).isEqualTo(savedVersion);
        assertThat(unchanged.getSummaryUpdatedAt()).isEqualTo(savedTimestamp);
        assertThat(model.calls).isEqualTo(1);

        entries.add(second);
        assertThat(service.handleRequest(request)).isEqualTo("Combined summary");
        assertThat(model.histories.getLast()).contains("First summary", "Sequence 2").doesNotContain("Sequence 1");
        assertThat(model.calls).isEqualTo(2);
        verify(history, times(2)).fetchAfter(42, 1);
        assertThat(store.load(42, PATH_SUMMARY).lastProcessedSequence()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void updateReplacesTextAdvancesCheckpointAndInvalidatesOldAudio() {
        var created = store.saveUpdatedSummary(SummarySnapshot.empty(42, PATH_SUMMARY), "Old", 2);
        var path = repository.findById(created.id()).orElseThrow();
        path.setAudioBlob(new byte[]{1, 2, 3});
        path.setAudioBlobUpdatedAt(LocalDateTime.now());
        repository.saveAndFlush(path);
        var expected = store.load(42, PATH_SUMMARY);
        store.saveUpdatedSummary(expected, "Updated", 4);
        var updated = repository.findById(created.id()).orElseThrow();
        assertThat(updated.getSummary()).isEqualTo("Updated");
        assertThat(updated.getLastProcessedSequence()).isEqualTo(4);
        assertThat(updated.getVersion()).isGreaterThan(expected.version());
        assertThat(updated.getSummaryUpdatedAt()).isNotNull();
        assertThat(updated.getAudioBlob()).isNull();
        assertThat(updated.getAudioBlobUpdatedAt()).isNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void staleGeneratedSummaryCannotOverwriteTheWinningRequest() {
        var first = store.saveUpdatedSummary(SummarySnapshot.empty(42, PATH_SUMMARY), "Original", 1);
        store.saveUpdatedSummary(first, "Winning update", 3);
        assertThatThrownBy(() -> store.saveUpdatedSummary(first, "Stale update", 2))
                .isInstanceOf(RecapConflictException.class);
        assertThat(store.load(42, PATH_SUMMARY).summary()).isEqualTo("Winning update");
        assertThat(store.load(42, PATH_SUMMARY).lastProcessedSequence()).isEqualTo(3);
    }

    @Test
    void twoFirstRequestsCannotCreateDuplicateRows() throws Exception {
        var empty = SummarySnapshot.empty(42, PATH_SUMMARY);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> save = () -> {
                start.await();
                try { store.saveUpdatedSummary(empty, "Summary", 1); return true; }
                catch (RecapConflictException | org.springframework.dao.DataIntegrityViolationException exception) { return false; }
            };
            var first = executor.submit(save);
            var second = executor.submit(save);
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(repository.count()).isEqualTo(1);
        assertThat(store.load(42, PATH_SUMMARY).lastProcessedSequence()).isEqualTo(1);
    }

    @Test
    void formatsHaveIndependentSavedNarrativesAndCheckpoints() {
        store.saveUpdatedSummary(SummarySnapshot.empty(42, PATH_SUMMARY), "Summary", 5);
        store.saveUpdatedSummary(SummarySnapshot.empty(42, CHARACTER_RECAP), "Recap", 2);
        assertThat(store.load(42, PATH_SUMMARY).lastProcessedSequence()).isEqualTo(5);
        assertThat(store.load(42, CHARACTER_RECAP).lastProcessedSequence()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void invalidUpdateLeavesPreviousSummaryAndCheckpointUntouched() {
        var existing = store.saveUpdatedSummary(SummarySnapshot.empty(42, PATH_SUMMARY), "Original", 2);
        assertThatThrownBy(() -> store.saveUpdatedSummary(existing, "", 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.saveUpdatedSummary(existing, "Invalid cursor", 2)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.load(42, PATH_SUMMARY)).isEqualTo(existing);
    }

    @Configuration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = CharacterPathRepository.class)
    @Import(CharacterPathService.class)
    static class DatabaseConfig {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:recap-store;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(CharacterPath.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}
