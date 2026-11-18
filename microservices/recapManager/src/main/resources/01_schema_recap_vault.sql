-- Recap Manager owns journey records, generated summaries and cached audio.
-- Raw journey entries belong to StoryPathManager and are retrieved over gRPC.
-- Each generated format stores its own sequence checkpoint.
CREATE SCHEMA IF NOT EXISTS `recap-vault` DEFAULT CHARACTER SET utf8mb3;
USE `recap-vault`;

CREATE TABLE IF NOT EXISTS `character_path` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `character_id` INT NOT NULL,
    `request_type` VARCHAR(30) NOT NULL DEFAULT 'PATH_SUMMARY',
    `last_processed_sequence` BIGINT NOT NULL DEFAULT 0,
    `version` BIGINT NOT NULL DEFAULT 0,
    `summary` LONGTEXT NULL DEFAULT NULL,
    `audio_blob` MEDIUMBLOB NULL,
    `summary_updated_at` DATETIME NULL DEFAULT NULL,
    `audio_blob_updated_at` DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_character_path_format` (`character_id`, `request_type`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb3;
