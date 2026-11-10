-- Recap Manager owns journey records, generated summaries and cached audio.
-- Character and choice IDs refer to data owned by the game service, so they
-- deliberately have no foreign keys to character_avatar or choice here.
CREATE SCHEMA IF NOT EXISTS `recap-vault` DEFAULT CHARACTER SET utf8mb3;
USE `recap-vault`;

CREATE TABLE IF NOT EXISTS `character_path` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `character_id` INT NOT NULL,
    `summary` LONGTEXT NULL DEFAULT NULL,
    `audio_blob` MEDIUMBLOB NULL,
    `summary_updated_at` DATETIME NULL DEFAULT NULL,
    `audio_blob_updated_at` DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `character_id_idx` (`character_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb3;

CREATE TABLE IF NOT EXISTS `character_path_choice` (
    `character_path_id` INT NOT NULL,
    `choice_id` INT NOT NULL,
    PRIMARY KEY (`character_path_id`, `choice_id`),
    INDEX `choice_id_idx` (`choice_id`),
    CONSTRAINT `fk_character_path_choice_character_path`
        FOREIGN KEY (`character_path_id`)
        REFERENCES `character_path` (`id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb3;

-- Paths and choice history must be populated through service integration.
-- The monolith's character-creation and gameplay triggers do not run here.
