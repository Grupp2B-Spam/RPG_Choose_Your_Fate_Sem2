package dk.ek.gruppe2.ttsmanager;

import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Atomic lease updates coordinate replicas without holding a transaction during paid generation. */
public final class JobStore {
    public record Job(SpeechSpec spec, String state, int attempts) {}
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lease;

    public JobStore(JdbcTemplate jdbc, Clock clock, int maxAttempts, Duration lease) {
        this.jdbc = jdbc; this.clock = clock; this.maxAttempts = maxAttempts; this.lease = lease;
    }

    public Job ensure(SpeechSpec spec) {
        Timestamp now = now();
        try {
            jdbc.update("INSERT INTO speech_job (content_hash,text,voice_id,model,output_format,state,created_at,updated_at) VALUES (?,?,?,?,?,'QUEUED',?,?)",
                    spec.contentHash(), spec.text(), spec.voiceId(), spec.model(), spec.outputFormat(), now, now);
        } catch (DuplicateKeyException duplicate) { /* Content hash is the idempotency key. */ }
        return get(spec.contentHash());
    }

    public Job get(String hash) {
        // A crashed final attempt must eventually become FAILED instead of staying PENDING forever.
        jdbc.update("UPDATE speech_job SET state=CASE WHEN attempts>=? THEN 'FAILED' ELSE 'QUEUED' END, error_code='LEASE_EXPIRED', lease_token=NULL, lease_until=NULL, updated_at=? WHERE content_hash=? AND state='PROCESSING' AND lease_until<?",
                maxAttempts, now(), hash, now());
        return jdbc.queryForObject("SELECT * FROM speech_job WHERE content_hash=?", (row, index) ->
                new Job(new SpeechSpec(row.getString("content_hash"), row.getString("text"), row.getString("voice_id"),
                        row.getString("model"), row.getString("output_format")), row.getString("state"), row.getInt("attempts")), hash);
    }

    public String claim(String hash) {
        String token = UUID.randomUUID().toString();
        int changed = jdbc.update("UPDATE speech_job SET state='PROCESSING', attempts=attempts+1, lease_token=?, lease_until=?, error_code=NULL, updated_at=? WHERE content_hash=? AND state<>'READY' AND attempts<? AND (state<>'PROCESSING' OR lease_until<?)",
                token, Timestamp.from(clock.instant().plus(lease)), now(), hash, maxAttempts, now());
        return changed == 1 ? token : null;
    }

    public boolean ready(String hash, String token) {
        return jdbc.update("UPDATE speech_job SET state='READY', lease_token=NULL, lease_until=NULL, error_code=NULL, updated_at=? WHERE content_hash=? AND state='PROCESSING' AND lease_token=?",
                now(), hash, token) == 1;
    }

    public void failed(String hash, String token) {
        jdbc.update("UPDATE speech_job SET state=CASE WHEN attempts>=? THEN 'FAILED' ELSE 'QUEUED' END, lease_token=NULL, lease_until=NULL, error_code='GENERATION_FAILED', updated_at=? WHERE content_hash=? AND state='PROCESSING' AND lease_token=?",
                maxAttempts, now(), hash, token);
    }

    private Timestamp now() { return Timestamp.from(clock.instant()); }
}
