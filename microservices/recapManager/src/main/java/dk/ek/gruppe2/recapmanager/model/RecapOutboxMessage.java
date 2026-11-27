package dk.ek.gruppe2.recapmanager.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(name = "recap_event_outbox")
@Getter
public class RecapOutboxMessage {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(nullable = false) private int attempts;
    @Version private Long version;

    protected RecapOutboxMessage() {}

    public RecapOutboxMessage(String id, String payload, Instant now) {
        this.id = id;
        this.payload = payload;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    public void retryAt(Instant nextAttempt) {
        attempts++;
        nextAttemptAt = nextAttempt;
    }
}
