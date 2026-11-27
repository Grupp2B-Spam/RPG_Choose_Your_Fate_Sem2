-- Additive setup, safe for an existing recap vault. Does not change journey/narrative data.
CREATE TABLE IF NOT EXISTS recap_event_outbox (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payload LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    next_attempt_at TIMESTAMP(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    INDEX ix_recap_outbox_retry (next_attempt_at, created_at)
);
