-- TextToSpeechManager owns this database. Audio bytes belong in MinIO, never here.
CREATE TABLE IF NOT EXISTS speech_job (
    content_hash CHAR(64) NOT NULL PRIMARY KEY,
    text LONGTEXT NOT NULL,
    voice_id VARCHAR(128) NOT NULL,
    model VARCHAR(128) NOT NULL,
    output_format VARCHAR(64) NOT NULL,
    state VARCHAR(16) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    lease_token VARCHAR(36),
    lease_until TIMESTAMP(6) NULL,
    error_code VARCHAR(64),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);
