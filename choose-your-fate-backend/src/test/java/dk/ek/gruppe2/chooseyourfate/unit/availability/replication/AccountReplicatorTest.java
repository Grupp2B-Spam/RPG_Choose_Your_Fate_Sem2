package dk.ek.gruppe2.chooseyourfate.unit.availability.replication;

import dk.ek.gruppe2.chooseyourfate.availability.replication.replicators.AccountReplicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountReplicatorTest {

    // ---------- Valid partitions ----------

    @Test
    void createAccountIsUpsertedIntoSecondary() {
        // Arrange
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
        AccountReplicator replicator = new AccountReplicator(jdbcTemplate);

        // Act
        replicator.create(Map.of(
                "id", 1,
                "username", "player",
                "characterLimit", 3,
                "email", "player@test.dk",
                "password", "hashed",
                "role", "ROLE_USER"
        ));

        // Assert
        assertTrue(jdbcTemplate.sql.getFirst().contains("INSERT INTO account"));
        assertTrue(jdbcTemplate.sql.getFirst().contains("ON DUPLICATE KEY UPDATE"));
        assertEquals(List.of(1, "player", 3, "player@test.dk", "hashed", "ROLE_USER"),
                jdbcTemplate.args.getFirst());
    }

    @Test
    void updateAccountUpdatesProvidedFieldsOnly() {
        // Arrange
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
        AccountReplicator replicator = new AccountReplicator(jdbcTemplate);

        // Act
        replicator.update(Map.of(
                "id", 1,
                "username", "updated",
                "email", "updated@test.dk"
        ));

        // Assert
        assertEquals("UPDATE account SET username = ?, email = ? WHERE id = ?",
                jdbcTemplate.sql.getFirst());
        assertEquals(List.of("updated", "updated@test.dk", 1), jdbcTemplate.args.getFirst());
    }

    @Test
    void deleteAccountDeletesAccountFromSecondary() {
        // Arrange
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
        AccountReplicator replicator = new AccountReplicator(jdbcTemplate);

        // Act
        replicator.delete(Map.of("id", 1));

        // Assert
        assertEquals("DELETE FROM account WHERE id = ?", jdbcTemplate.sql.getFirst());
        assertEquals(List.of(1), jdbcTemplate.args.getFirst());
    }

    // ---------- Invalid partitions: missing required fields ----------

    @ParameterizedTest
    @ValueSource(strings = {"id", "username", "characterLimit", "email", "password", "role"})
    void createAccountWithMissingRequiredFieldShouldThrow(String missingField) {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", 1);
        payload.put("username", "player");
        payload.put("characterLimit", 3);
        payload.put("email", "p@test.dk");
        payload.put("password", "hashed");
        payload.put("role", "ROLE_USER");
        payload.remove(missingField);

        AccountReplicator replicator = new AccountReplicator(new CapturingJdbcTemplate());

        // Act / Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> replicator.create(payload));

        // Assert
        assertTrue(ex.getMessage().contains(missingField),
                "Error message should mention missing field: " + missingField);
    }

    @Test
    void updateAccountWithoutIdShouldThrow() {
        AccountReplicator replicator = new AccountReplicator(new CapturingJdbcTemplate());
        assertThrows(IllegalArgumentException.class,
                () -> replicator.update(Map.of("username", "x")));
    }

    @Test
    void deleteAccountWithoutIdShouldThrow() {
        AccountReplicator replicator = new AccountReplicator(new CapturingJdbcTemplate());
        assertThrows(IllegalArgumentException.class,
                () -> replicator.delete(Map.of()));
    }

    // ---------- Edge case ----------

    @Test
    void updateAccountWithOnlyIdShouldEmitNoSql() {
        // Arrange
        CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
        AccountReplicator replicator = new AccountReplicator(jdbcTemplate);

        // Act
        replicator.update(Map.of("id", 1));

        // Assert
        assertTrue(jdbcTemplate.sql.isEmpty(),
                "No SQL should be emitted when UPDATE has nothing to set");
    }

    // ---------- Helper ----------

    private static class CapturingJdbcTemplate extends JdbcTemplate {
        private final List<String> sql = new ArrayList<>();
        private final List<List<Object>> args = new ArrayList<>();

        @Override
        public int update(String sql, Object... args) {
            this.sql.add(sql.strip());
            this.args.add(List.of(args));
            return 1;
        }
    }
}
