-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema character-vault
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema character-vault
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `character-vault` DEFAULT CHARACTER SET utf8mb3 ;
USE `character-vault` ;

-- -----------------------------------------------------
-- Table `character-vault`.`race_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`race_details` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'BOB rules them all!',
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `starting_chapter_id` INT NOT NULL, -- needed to determine where the character starts on creating new character
  PRIMARY KEY (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`character_avatar`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`character_avatar` (
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
  CONSTRAINT `fk_race_detail_character_avatar`
    FOREIGN KEY (`race_detail_id`)
    REFERENCES `character-vault`.`race_details` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`character_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`character_details` (
  `character_id` INT NOT NULL,
  `intelligence` INT NULL DEFAULT NULL,
  `charisma` INT NULL DEFAULT NULL,
  `fashion` INT NULL DEFAULT NULL,
  PRIMARY KEY (`character_id`),
  CONSTRAINT `fk_character_details_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `character-vault`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`item` (
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
-- Table `character-vault`.`equipment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`equipment` (
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
    REFERENCES `character-vault`.`character_avatar` (`id`),
  CONSTRAINT `fk_equipment_chest`
    FOREIGN KEY (`chest`)
    REFERENCES `character-vault`.`item` (`id`),
  CONSTRAINT `fk_equipment_head`
    FOREIGN KEY (`head`)
    REFERENCES `character-vault`.`item` (`id`),
  CONSTRAINT `fk_equipment_legs`
    FOREIGN KEY (`legs`)
    REFERENCES `character-vault`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`inventory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`inventory` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `character_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_inventory_character1_idx` (`character_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `character-vault`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`inventory_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`inventory_has_item` (
  `item_id` INT NOT NULL,
  `inventory_id` INT NOT NULL,
  `amount` INT NULL DEFAULT NULL,
  PRIMARY KEY (`item_id`, `inventory_id`),
  INDEX `fk_item_has_inventory_inventory1_idx` (`inventory_id` ASC) VISIBLE,
  INDEX `fk_item_has_inventory_item1_idx` (`item_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_has_item_inventory`
    FOREIGN KEY (`inventory_id`)
    REFERENCES `character-vault`.`inventory` (`id`),
  CONSTRAINT `fk_inventory_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `character-vault`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`quest`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`quest` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `scene_id` INT NOT NULL,
    `description` MEDIUMTEXT NULL DEFAULT NULL,
    PRIMARY KEY (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

CREATE TABLE IF NOT EXISTS `character-vault`.`character_has_quest` (
    `character_id` INT NOT NULL,
    `quest_id` INT NOT NULL,
    `status` TINYINT NOT NULL,
    PRIMARY KEY (`character_id`, `quest_id`),
    INDEX `quest_id_idx` (`quest_id` ASC) VISIBLE,
    CONSTRAINT `fk_character_has_quest_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `character-vault`.`character_avatar` (`id`),
    CONSTRAINT `fk_character_has_quest_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `character-vault`.`quest` (`id`)
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `character-vault`.`quest_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `character-vault`.`quest_has_item` (
    `quest_id` INT NOT NULL,
    `item_id` INT NOT NULL,
    PRIMARY KEY (`quest_id`, `item_id`),
    INDEX `fk_quest_has_item_item1_idx` (`item_id` ASC) VISIBLE,
    INDEX `fk_quest_has_item_quest1_idx` (`quest_id` ASC) VISIBLE,
    CONSTRAINT `fk_quest_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `character-vault`.`item` (`id`),
    CONSTRAINT `fk_quest_has_item_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `character-vault`.`quest` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- View `character-vault`.`v_character`
-- -----------------------------------------------------
DROP VIEW IF EXISTS `character-vault`.`v_character`;
USE `character-vault`;
CREATE  OR REPLACE VIEW `v_character` AS (
    SELECT avatar.name, deets.intelligence, deets.charisma, deets.fashion, avatar.flag
    FROM character_details deets
    JOIN character_avatar avatar
      ON deets.character_id = avatar.id
        );

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
