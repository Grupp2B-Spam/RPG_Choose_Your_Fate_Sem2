-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema choose_your_fate
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema choose_your_fate
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `choose_your_fate` DEFAULT CHARACTER SET utf8mb3 ;
USE `choose_your_fate` ;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`account`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`account` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `character_limit` INT NOT NULL,
  `email` VARCHAR(100) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`audit_log`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`audit_log` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `table_name` VARCHAR(100) NOT NULL,
  `entity_id` VARCHAR(100) NOT NULL,
  `action_type` VARCHAR(20) NOT NULL,
  `database_user` VARCHAR(100) NOT NULL,
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `old_data` JSON NULL,
  `new_data` JSON NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_audit_log_table_name` (`table_name` ASC) VISIBLE,
  INDEX `idx_audit_log_entity_id` (`entity_id` ASC) VISIBLE,
  INDEX `idx_audit_log_changed_at` (`changed_at` ASC) VISIBLE
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`chapter`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`chapter` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `starting_scene_id` INT NULL, -- Has to not be NOT NULL, since it otherwise creates a loop with scenes table which needs a chapter to be created
  PRIMARY KEY (`id`),
  INDEX `starting_scene_idx` (`starting_scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_starting_scene_chapter`
    FOREIGN KEY (`starting_scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`race_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`race_details` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'BOB rules them all!',
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `starting_chapter_id` INT NOT NULL, -- needed to determine where the character starts on creating new character
  PRIMARY KEY (`id`),
  INDEX `starting_chapter_idx` (`starting_chapter_id` ASC) VISIBLE,
  CONSTRAINT `fk_starting_chapter_race_details`
    FOREIGN KEY (`starting_chapter_id`)
    REFERENCES `choose_your_fate`.`chapter` (`id`)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `choose_your_fate`.`sceneId`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`sceneId` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `chapter_id` INT NOT NULL,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `chapter_id_idx` (`chapter_id` ASC) VISIBLE,
  CONSTRAINT `fk_scene_chapter`
    FOREIGN KEY (`chapter_id`)
    REFERENCES `choose_your_fate`.`chapter` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`character_avatar`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`character_avatar` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `account_id` INT NOT NULL,
  `chapter_id` INT NOT NULL,
  `scene_id` INT NOT NULL,
  `race_detail_id` INT NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `flag` JSON NOT NULL COMMENT 'Lost/gain of rep, debuff/buff, BOB?... etc',
  PRIMARY KEY (`id`),
  INDEX `player_id_idx` (`account_id` ASC) VISIBLE,
  INDEX `chapter_id_idx` (`chapter_id` ASC) VISIBLE,
  INDEX `scene_id_idx` (`scene_id` ASC) VISIBLE,
  INDEX `race_detail_id_idx` (`race_detail_id` ASC) VISIBLE,
  CONSTRAINT `fk_chapter_character_avatar`
    FOREIGN KEY (`chapter_id`)
    REFERENCES `choose_your_fate`.`chapter` (`id`),
  CONSTRAINT `fk_player_character_avatar`
    FOREIGN KEY (`account_id`)
    REFERENCES `choose_your_fate`.`account` (`id`),
  CONSTRAINT `fk_race_detail_character_avatar`
    FOREIGN KEY (`race_detail_id`)
    REFERENCES `choose_your_fate`.`race_details` (`id`),
  CONSTRAINT `fk_scene_character_avatar`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`character_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`character_details` (
  `character_id` INT NOT NULL,
  `intelligence` INT NULL DEFAULT NULL,
  `charisma` INT NULL DEFAULT NULL,
  `fashion` INT NULL DEFAULT NULL,
  PRIMARY KEY (`character_id`),
  CONSTRAINT `fk_character_details_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`quest`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`quest` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `scene_id` INT NOT NULL,
  `description` MEDIUMTEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `scene_id_idx` (`scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_quest_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`character_has_quest`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`character_has_quest` (
  `character_id` INT NOT NULL,
  `quest_id` INT NOT NULL,
  `status` TINYINT NOT NULL,
  PRIMARY KEY (`character_id`, `quest_id`),
  INDEX `quest_id_idx` (`quest_id` ASC) VISIBLE,
  CONSTRAINT `fk_character_has_quest_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate`.`character_avatar` (`id`),
  CONSTRAINT `fk_character_has_quest_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `choose_your_fate`.`quest` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`character_path`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`character_path` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `character_id` INT NOT NULL,
  `summary` LONGTEXT NULL DEFAULT NULL,
  `audio_blob` mediumblob,
  `summary_updated_at` datetime DEFAULT NULL,
  `audio_blob_updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `character_id_idx` (`character_id` ASC) VISIBLE,
  CONSTRAINT `fk_character_path_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`choice`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`choice` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `scene_id` INT NOT NULL,
  `destination_scene_id` INT NULL DEFAULT NULL,
  `description` MEDIUMTEXT NULL DEFAULT NULL,
  `consequence` VARCHAR(50) NULL,
  `target_id` INT NULL COMMENT 'Give quest, give item(s)... etc',
  `value_int` INT NULL COMMENT 'Stat change, lose hp, fashion!!!... etc\n\n\n\n\nze BOB race!',
  `story_weight` SMALLINT(255) NOT NULL COMMENT 'Value/weight of the recap or story telling for the AI to use.',
  `requirements` JSON NULL COMMENT 'Requirements is what gives or takes away choices/ quests because you either meet the requirements or dont',
  PRIMARY KEY (`id`),
  INDEX `scene_id_idx` (`scene_id` ASC) VISIBLE,
  INDEX `fk_choice_destination_scene_idx` (`destination_scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_choice_destination_scene`
    FOREIGN KEY (`destination_scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`),
  CONSTRAINT `fk_choice_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`character_path_choice`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`character_path_choice` (
  `character_path_id` INT NOT NULL,
  `choice_id` INT NOT NULL,
  PRIMARY KEY (`character_path_id`, `choice_id`),
  INDEX `choice_id_idx` (`choice_id` ASC) VISIBLE,
  CONSTRAINT `fk_character_path_choice_character_path`
    FOREIGN KEY (`character_path_id`)
    REFERENCES `choose_your_fate`.`character_path` (`id`),
  CONSTRAINT `fk_character_path_choice_choice`
    FOREIGN KEY (`choice_id`)
    REFERENCES `choose_your_fate`.`choice` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`item` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NULL DEFAULT NULL,
  `description` TEXT NULL DEFAULT NULL,
  `type` VARCHAR(45) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_item_type` (`type` ASC) VISIBLE
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`equipment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`equipment` (
  `character_id` INT NOT NULL,
  `head` INT NULL DEFAULT NULL,
  `legs` INT NULL DEFAULT NULL,
  `chest` INT NULL DEFAULT NULL,
  PRIMARY KEY (`character_id`),
  INDEX `fk_equipment_character1_idx` (`character_id` ASC) VISIBLE,
  INDEX `fk_equipment_item_idx` (`head` ASC) VISIBLE,
  INDEX `fk_equipment_chest_idx` (`chest` ASC) VISIBLE,
  INDEX `fk_equipment_legs_idx` (`legs` ASC) VISIBLE,
  CONSTRAINT `fk_equipment_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate`.`character_avatar` (`id`),
  CONSTRAINT `fk_equipment_chest`
    FOREIGN KEY (`chest`)
    REFERENCES `choose_your_fate`.`item` (`id`),
  CONSTRAINT `fk_equipment_head`
    FOREIGN KEY (`head`)
    REFERENCES `choose_your_fate`.`item` (`id`),
  CONSTRAINT `fk_equipment_legs`
    FOREIGN KEY (`legs`)
    REFERENCES `choose_your_fate`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`inventory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`inventory` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `character_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_inventory_character1_idx` (`character_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`inventory_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`inventory_has_item` (
  `item_id` INT NOT NULL,
  `inventory_id` INT NOT NULL,
  `amount` INT NULL DEFAULT NULL,
  PRIMARY KEY (`item_id`, `inventory_id`),
  INDEX `fk_item_has_inventory_inventory1_idx` (`inventory_id` ASC) VISIBLE,
  INDEX `fk_item_has_inventory_item1_idx` (`item_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_has_item_inventory`
    FOREIGN KEY (`inventory_id`)
    REFERENCES `choose_your_fate`.`inventory` (`id`),
  CONSTRAINT `fk_inventory_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`npc`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`npc` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `race_details_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_race_details_idx` (`race_details_id` ASC) VISIBLE,
  CONSTRAINT `fk_npc_race_details`
    FOREIGN KEY (`race_details_id`)
    REFERENCES `choose_your_fate`.`race_details` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`quest_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`quest_has_item` (
  `quest_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  PRIMARY KEY (`quest_id`, `item_id`),
  INDEX `fk_quest_has_item_item1_idx` (`item_id` ASC) VISIBLE,
  INDEX `fk_quest_has_item_quest1_idx` (`quest_id` ASC) VISIBLE,
  CONSTRAINT `fk_quest_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate`.`item` (`id`),
  CONSTRAINT `fk_quest_has_item_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `choose_your_fate`.`quest` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`scene_has_npc`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`scene_has_npc` (
  `scene_id` INT NOT NULL,
  `npc_id` INT NOT NULL,
  PRIMARY KEY (`scene_id`, `npc_id`),
  INDEX `fk_scene_has_npc_npc1_idx` (`npc_id` ASC) VISIBLE,
  INDEX `fk_scene_has_npc_scene1_idx` (`scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_scene_has_npc_npc`
    FOREIGN KEY (`npc_id`)
    REFERENCES `choose_your_fate`.`npc` (`id`),
  CONSTRAINT `fk_scene_has_npc_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate`.`sceneId` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate`.`choice_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate`.`choice_has_item` (
  `choice_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  PRIMARY KEY (`choice_id`, `item_id`),
  INDEX `fk_choice_has_item_item1_idx` (`item_id` ASC) VISIBLE,
  INDEX `fk_choice_has_item_choice1_idx` (`choice_id` ASC) VISIBLE,
  CONSTRAINT `fk_choice_has_item_choice1`
    FOREIGN KEY (`choice_id`)
    REFERENCES `choose_your_fate`.`choice` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_choice_has_item_item1`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate`.`item` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

USE `choose_your_fate`;

-- -----------------------------------------------------
-- View `choose_your_fate`.`v_character`
-- -----------------------------------------------------
DROP VIEW IF EXISTS `choose_your_fate`.`v_character`;
USE `choose_your_fate`;
CREATE  OR REPLACE VIEW `v_character` AS (
    SELECT avatar.name, deets.intelligence, deets.charisma, deets.fashion, avatar.flag
    FROM character_details deets
    JOIN character_avatar avatar
      ON deets.character_id = avatar.id
        );

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
