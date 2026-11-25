package dk.ek.gruppe2.chooseyourfate.availability.replication.replicators;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AccountReplicator implements EntityReplicator {

    private final JdbcTemplate secondaryJdbcTemplate;

    public AccountReplicator(@Qualifier("secondaryJdbcTemplate") JdbcTemplate secondaryJdbcTemplate) {
        this.secondaryJdbcTemplate = secondaryJdbcTemplate;
    }

    @Override
    public String entityName() {
        return "account";
    }

    @Override
    public void create(Map<String, Object> payload) {
        secondaryJdbcTemplate.update(
                """
                INSERT INTO account (id, username, character_limit, email, password, role)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    character_limit = VALUES(character_limit),
                    email = VALUES(email),
                    password = VALUES(password),
                    role = VALUES(role)
                """,
                required(payload, "id"),
                required(payload, "username"),
                required(payload, "characterLimit"),
                required(payload, "email"),
                required(payload, "password"),
                required(payload, "role")
        );
    }

    @Override
    public void update(Map<String, Object> payload) {
        Object id = required(payload, "id");
        List<String> assignments = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        addAssignment(payload, assignments, values, "username", "username");
        addAssignment(payload, assignments, values, "characterLimit", "character_limit");
        addAssignment(payload, assignments, values, "email", "email");
        addAssignment(payload, assignments, values, "password", "password");
        addAssignment(payload, assignments, values, "role", "role");

        if (assignments.isEmpty()) {
            return;
        }

        values.add(id);
        secondaryJdbcTemplate.update(
                "UPDATE account SET " + String.join(", ", assignments) + " WHERE id = ?",
                values.toArray()
        );
    }

    @Override
    public void delete(Map<String, Object> payload) {
        secondaryJdbcTemplate.update("DELETE FROM account WHERE id = ?", required(payload, "id"));
    }

    private void addAssignment(
            Map<String, Object> payload,
            List<String> assignments,
            List<Object> values,
            String payloadKey,
            String columnName
    ) {
        if (payload.containsKey(payloadKey)) {
            assignments.add(columnName + " = ?");
            values.add(payload.get(payloadKey));
        }
    }

    private Object required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Replication payload is missing required " + entityName() + " field: " + key);
        }
        return value;
    }
}
