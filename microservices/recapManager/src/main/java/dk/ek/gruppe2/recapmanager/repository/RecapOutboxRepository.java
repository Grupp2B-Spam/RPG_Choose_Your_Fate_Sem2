package dk.ek.gruppe2.recapmanager.repository;

import dk.ek.gruppe2.recapmanager.model.RecapOutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface RecapOutboxRepository extends JpaRepository<RecapOutboxMessage, String> {
    List<RecapOutboxMessage> findTop25ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant now);
}
