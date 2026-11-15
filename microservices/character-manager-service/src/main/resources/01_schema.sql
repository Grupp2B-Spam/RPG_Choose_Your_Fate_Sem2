SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema choose_your_fate_test
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema choose_your_fate_test
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `choose_your_fate_test` DEFAULT CHARACTER SET utf8mb3 ;
USE `choose_your_fate_test` ;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`account`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`account` (
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
-- Table `choose_your_fate_test`.`audit_log`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`audit_log` (
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
-- Table `choose_your_fate_test`.`chapter`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`chapter` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `starting_scene_id` INT NULL, -- Has to not be NOT NULL, since it otherwise creates a loop with scenes table which needs a chapter to be created
  PRIMARY KEY (`id`),
  INDEX `starting_scene_idx` (`starting_scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_starting_scene_chapter`
    FOREIGN KEY (`starting_scene_id`)
    REFERENCES `choose_your_fate_test`.`scene` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`race_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`race_details` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT 'BOB rules them all!',
  `name` VARCHAR(100) NULL DEFAULT NULL,
  `starting_chapter_id` INT NOT NULL, -- needed to determine where the character starts on creating new character
  PRIMARY KEY (`id`),
  INDEX `starting_chapter_idx` (`starting_chapter_id` ASC) VISIBLE,
  CONSTRAINT `fk_starting_chapter_race_details`
    FOREIGN KEY (`starting_chapter_id`)
    REFERENCES `choose_your_fate_test`.`chapter` (`id`)
  )
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;


-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`scene`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`scene` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `chapter_id` INT NOT NULL,
  `name` VARCHAR(100) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `chapter_id_idx` (`chapter_id` ASC) VISIBLE,
  CONSTRAINT `fk_scene_chapter`
    FOREIGN KEY (`chapter_id`)
    REFERENCES `choose_your_fate_test`.`chapter` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`character_avatar`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`character_avatar` (
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
    REFERENCES `choose_your_fate_test`.`chapter` (`id`),
  CONSTRAINT `fk_player_character_avatar`
    FOREIGN KEY (`account_id`)
    REFERENCES `choose_your_fate_test`.`account` (`id`),
  CONSTRAINT `fk_race_detail_character_avatar`
    FOREIGN KEY (`race_detail_id`)
    REFERENCES `choose_your_fate_test`.`race_details` (`id`),
  CONSTRAINT `fk_scene_character_avatar`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate_test`.`scene` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`character_details`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`character_details` (
  `character_id` INT NOT NULL,
  `intelligence` INT NULL DEFAULT NULL,
  `charisma` INT NULL DEFAULT NULL,
  `fashion` INT NULL DEFAULT NULL,
  PRIMARY KEY (`character_id`),
  CONSTRAINT `fk_character_details_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate_test`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`quest`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`quest` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `scene_id` INT NOT NULL,
  `description` MEDIUMTEXT NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `scene_id_idx` (`scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_quest_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate_test`.`scene` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`character_has_quest`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`character_has_quest` (
  `character_id` INT NOT NULL,
  `quest_id` INT NOT NULL,
  `status` TINYINT NOT NULL,
  PRIMARY KEY (`character_id`, `quest_id`),
  INDEX `quest_id_idx` (`quest_id` ASC) VISIBLE,
  CONSTRAINT `fk_character_has_quest_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate_test`.`character_avatar` (`id`),
  CONSTRAINT `fk_character_has_quest_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `choose_your_fate_test`.`quest` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`character_path`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`character_path` (
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
    REFERENCES `choose_your_fate_test`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`choice`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`choice` (
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
    REFERENCES `choose_your_fate_test`.`scene` (`id`),
  CONSTRAINT `fk_choice_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate_test`.`scene` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`character_path_choice`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`character_path_choice` (
  `character_path_id` INT NOT NULL,
  `choice_id` INT NOT NULL,
  PRIMARY KEY (`character_path_id`, `choice_id`),
  INDEX `choice_id_idx` (`choice_id` ASC) VISIBLE,
  CONSTRAINT `fk_character_path_choice_character_path`
    FOREIGN KEY (`character_path_id`)
    REFERENCES `choose_your_fate_test`.`character_path` (`id`),
  CONSTRAINT `fk_character_path_choice_choice`
    FOREIGN KEY (`choice_id`)
    REFERENCES `choose_your_fate_test`.`choice` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`item` (
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
-- Table `choose_your_fate_test`.`equipment`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`equipment` (
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
    REFERENCES `choose_your_fate_test`.`character_avatar` (`id`),
  CONSTRAINT `fk_equipment_chest`
    FOREIGN KEY (`chest`)
    REFERENCES `choose_your_fate_test`.`item` (`id`),
  CONSTRAINT `fk_equipment_head`
    FOREIGN KEY (`head`)
    REFERENCES `choose_your_fate_test`.`item` (`id`),
  CONSTRAINT `fk_equipment_legs`
    FOREIGN KEY (`legs`)
    REFERENCES `choose_your_fate_test`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`inventory`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`inventory` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `character_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_inventory_character1_idx` (`character_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_character`
    FOREIGN KEY (`character_id`)
    REFERENCES `choose_your_fate_test`.`character_avatar` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`inventory_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`inventory_has_item` (
  `item_id` INT NOT NULL,
  `inventory_id` INT NOT NULL,
  `amount` INT NULL DEFAULT NULL,
  PRIMARY KEY (`item_id`, `inventory_id`),
  INDEX `fk_item_has_inventory_inventory1_idx` (`inventory_id` ASC) VISIBLE,
  INDEX `fk_item_has_inventory_item1_idx` (`item_id` ASC) VISIBLE,
  CONSTRAINT `fk_inventory_has_item_inventory`
    FOREIGN KEY (`inventory_id`)
    REFERENCES `choose_your_fate_test`.`inventory` (`id`),
  CONSTRAINT `fk_inventory_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate_test`.`item` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`npc`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`npc` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  `race_details_id` INT NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_race_details_idx` (`race_details_id` ASC) VISIBLE,
  CONSTRAINT `fk_npc_race_details`
    FOREIGN KEY (`race_details_id`)
    REFERENCES `choose_your_fate_test`.`race_details` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`quest_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`quest_has_item` (
  `quest_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  PRIMARY KEY (`quest_id`, `item_id`),
  INDEX `fk_quest_has_item_item1_idx` (`item_id` ASC) VISIBLE,
  INDEX `fk_quest_has_item_quest1_idx` (`quest_id` ASC) VISIBLE,
  CONSTRAINT `fk_quest_has_item_item`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate_test`.`item` (`id`),
  CONSTRAINT `fk_quest_has_item_quest`
    FOREIGN KEY (`quest_id`)
    REFERENCES `choose_your_fate_test`.`quest` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`scene_has_npc`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`scene_has_npc` (
  `scene_id` INT NOT NULL,
  `npc_id` INT NOT NULL,
  PRIMARY KEY (`scene_id`, `npc_id`),
  INDEX `fk_scene_has_npc_npc1_idx` (`npc_id` ASC) VISIBLE,
  INDEX `fk_scene_has_npc_scene1_idx` (`scene_id` ASC) VISIBLE,
  CONSTRAINT `fk_scene_has_npc_npc`
    FOREIGN KEY (`npc_id`)
    REFERENCES `choose_your_fate_test`.`npc` (`id`),
  CONSTRAINT `fk_scene_has_npc_scene`
    FOREIGN KEY (`scene_id`)
    REFERENCES `choose_your_fate_test`.`scene` (`id`)
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

-- -----------------------------------------------------
-- Table `choose_your_fate_test`.`choice_has_item`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `choose_your_fate_test`.`choice_has_item` (
  `choice_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  PRIMARY KEY (`choice_id`, `item_id`),
  INDEX `fk_choice_has_item_item1_idx` (`item_id` ASC) VISIBLE,
  INDEX `fk_choice_has_item_choice1_idx` (`choice_id` ASC) VISIBLE,
  CONSTRAINT `fk_choice_has_item_choice1`
    FOREIGN KEY (`choice_id`)
    REFERENCES `choose_your_fate_test`.`choice` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_choice_has_item_item1`
    FOREIGN KEY (`item_id`)
    REFERENCES `choose_your_fate_test`.`item` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb3;

USE `choose_your_fate_test`;

-- -----------------------------------------------------
-- View `choose_your_fate_test`.`v_character`
-- -----------------------------------------------------
DROP VIEW IF EXISTS `choose_your_fate_test`.`v_character`;
USE `choose_your_fate_test`;
CREATE  OR REPLACE VIEW `v_character` AS (
    SELECT avatar.name, deets.intelligence, deets.charisma, deets.fashion, avatar.flag
    FROM character_details deets
    JOIN character_avatar avatar
      ON deets.character_id = avatar.id
        );


-- Procedures

DROP PROCEDURE IF EXISTS `sp_create_character`;
CREATE DEFINER=`root`@`%` PROCEDURE `sp_create_character`(
IN p_account_id INT,
IN p_chapter_id INT,
IN p_scene_id INT,
IN p_race_detail_id INT,
IN p_name varchar(50),
OUT p_character_id INT
)
BEGIN
DECLARE v_character_count INT;
DECLARE v_character_limit INT;
DECLARE v_scene_chapter_id INT;

SELECT character_limit INTO v_character_limit
FROM account WHERE id = p_account_id;

SELECT COUNT(id) INTO v_character_count
FROM `character_avatar` WHERE account_id = p_account_id;

SELECT chapter_id INTO v_scene_chapter_id
FROM scene
WHERE id = p_scene_id;

IF v_scene_chapter_id IS NULL THEN
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Selected scene does not exist.';
END IF;

IF v_scene_chapter_id != p_chapter_id THEN
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Selected scene does not belong to the selected chapter.';
END IF;

IF v_character_count < v_character_limit THEN
	INSERT INTO `character_avatar` (account_id, chapter_id, scene_id, race_detail_id, name, flag)
    VALUES (p_account_id, p_chapter_id, p_scene_id, p_race_detail_id, p_name, JSON_OBJECT());
    SET p_character_id = LAST_INSERT_ID();
ELSE
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Character limit reached.';
END IF;

END;

DROP PROCEDURE IF EXISTS `sp_delete_character`;
CREATE PROCEDURE `sp_delete_character`(IN p_character_id INT)
BEGIN
	DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
		ROLLBACK;
        RESIGNAL;
	END;
    START TRANSACTION;
		DELETE FROM character_has_quest WHERE character_id = p_character_id;
        
		DELETE FROM character_path_choice WHERE character_path_id IN (
			SELECT id FROM character_path WHERE character_path.character_id = p_character_id);
		
        DELETE FROM inventory_has_item WHERE inventory_id IN (
			SELECT id FROM inventory WHERE inventory.character_id = p_character_id);
			
        DELETE FROM character_path WHERE character_id = p_character_id;
        
        DELETE FROM inventory WHERE character_id = p_character_id;
        
        DELETE FROM character_details WHERE character_id = p_character_id;
        
        DELETE FROM equipment WHERE character_id = p_character_id;
        
        DELETE FROM `character_avatar` WHERE id = p_character_id;
    COMMIT;
END;

DROP PROCEDURE IF EXISTS `sp_make_choice`;
CREATE PROCEDURE `sp_make_choice`(IN p_character_id INT, IN p_choice_id INT)
BEGIN
    DECLARE v_character_scene_id INT;
    DECLARE v_choice_scene_id INT;
	DECLARE EXIT HANDLER FOR SQLEXCEPTION
	BEGIN
		ROLLBACK;
        RESIGNAL;
	END;
    START TRANSACTION;
    
    SELECT scene_id INTO v_character_scene_id FROM `character_avatar` WHERE `character_avatar`.id = p_character_id;
    SELECT scene_id INTO v_choice_scene_id FROM choice WHERE id = p_choice_id;
    
    IF v_character_scene_id != v_choice_scene_id THEN
		SIGNAL SQLSTATE '45000'
			SET MESSAGE_TEXT = 'Character is not in the correct scene for this choice.';
	END IF;	
    
    INSERT INTO character_path_choice (character_path_id, choice_id)
    VALUES (
		(SELECT id FROM character_path WHERE character_id = p_character_id),
        p_choice_id
    );
    
    UPDATE `character_avatar`
    SET
        scene_id = (SELECT destination_scene_id FROM choice WHERE id = p_choice_id),
        chapter_id = (
            SELECT s.chapter_id
            FROM scene s
            JOIN choice c ON c.destination_scene_id = s.id
            WHERE c.id = p_choice_id
        )
    WHERE id = p_character_id;
    
    COMMIT;
END;

DROP PROCEDURE IF EXISTS `sp_grant_quest_rewards`;
CREATE PROCEDURE `sp_grant_quest_rewards`(
IN p_character_id INT,
IN p_quest_id INT)
BEGIN
INSERT INTO inventory_has_item (item_id, inventory_id, amount)
	SELECT quest_has_item.item_id, inventory.id, 1 FROM quest_has_item
    INNER JOIN inventory ON inventory.character_id = p_character_id
    WHERE quest_has_item.quest_id = p_quest_id
ON DUPLICATE KEY UPDATE amount = amount + 1;
END;


-- functions

DROP FUNCTION IF EXISTS `fn_is_quest_complete`;
CREATE FUNCTION `fn_is_quest_complete`(p_character_id INT, p_quest_id INT) RETURNS tinyint(1)
    DETERMINISTIC
BEGIN
DECLARE v_quest_status BOOLEAN;
SELECT `status` INTO v_quest_status 
FROM character_has_quest WHERE p_character_id = character_id AND p_quest_id = quest_id;
RETURN v_quest_status;
END;

DROP FUNCTION IF EXISTS `fn_has_required_item`;
CREATE FUNCTION `fn_has_required_item`(p_character_id INT, p_item_id INT) RETURNS tinyint(1)
    DETERMINISTIC
BEGIN
DECLARE v_character_has_item BOOLEAN;
SELECT EXISTS (
	SELECT 1 FROM inventory_has_item
	INNER JOIN inventory on inventory.id = inventory_has_item.inventory_id
	WHERE inventory.character_id = p_character_id AND inventory_has_item.item_id = p_item_id
    ) INTO v_character_has_item;
RETURN v_character_has_item;
END;


-- triggers

DROP TRIGGER IF EXISTS `grant_rewards_after_quest_completion`;
CREATE TRIGGER `grant_rewards_after_quest_completion` AFTER UPDATE ON `character_has_quest` FOR EACH ROW 
BEGIN
IF NEW.status = 1 THEN
	CALL sp_grant_quest_rewards(NEW.character_id, NEW.quest_id);
END IF;
END;

DROP TRIGGER IF EXISTS `audit_account_after_insert`;
CREATE TRIGGER `audit_account_after_insert` AFTER INSERT ON `account` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, new_data)
VALUES (
    'account',
    NEW.id,
    'INSERT',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', NEW.id,
        'username', NEW.username,
        'character_limit', NEW.character_limit,
        'email', NEW.email,
        'role', NEW.role
    )
);
END;

DROP TRIGGER IF EXISTS `audit_account_after_update`;
CREATE TRIGGER `audit_account_after_update` AFTER UPDATE ON `account` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data, new_data)
VALUES (
    'account',
    NEW.id,
    'UPDATE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'username', OLD.username,
        'character_limit', OLD.character_limit,
        'email', OLD.email,
        'role', OLD.role
    ),
    JSON_OBJECT(
        'id', NEW.id,
        'username', NEW.username,
        'character_limit', NEW.character_limit,
        'email', NEW.email,
        'role', NEW.role
    )
);
END;

DROP TRIGGER IF EXISTS `audit_account_after_delete`;
CREATE TRIGGER `audit_account_after_delete` AFTER DELETE ON `account` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data)
VALUES (
    'account',
    OLD.id,
    'DELETE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'username', OLD.username,
        'character_limit', OLD.character_limit,
        'email', OLD.email,
        'role', OLD.role
    )
);
END;

DROP TRIGGER IF EXISTS `audit_character_avatar_after_insert`;
CREATE TRIGGER `audit_character_avatar_after_insert` AFTER INSERT ON `character_avatar` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, new_data)
VALUES (
    'character_avatar',
    NEW.id,
    'INSERT',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', NEW.id,
        'account_id', NEW.account_id,
        'chapter_id', NEW.chapter_id,
        'scene_id', NEW.scene_id,
        'race_detail_id', NEW.race_detail_id,
        'name', NEW.name,
        'flag', NEW.flag
    )
);
END;

DROP TRIGGER IF EXISTS `audit_character_avatar_after_update`;
CREATE TRIGGER `audit_character_avatar_after_update` AFTER UPDATE ON `character_avatar` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data, new_data)
VALUES (
    'character_avatar',
    NEW.id,
    'UPDATE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'account_id', OLD.account_id,
        'chapter_id', OLD.chapter_id,
        'scene_id', OLD.scene_id,
        'race_detail_id', OLD.race_detail_id,
        'name', OLD.name,
        'flag', OLD.flag
    ),
    JSON_OBJECT(
        'id', NEW.id,
        'account_id', NEW.account_id,
        'chapter_id', NEW.chapter_id,
        'scene_id', NEW.scene_id,
        'race_detail_id', NEW.race_detail_id,
        'name', NEW.name,
        'flag', NEW.flag
    )
);
END;

DROP TRIGGER IF EXISTS `audit_character_avatar_after_delete`;
CREATE TRIGGER `audit_character_avatar_after_delete` AFTER DELETE ON `character_avatar` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data)
VALUES (
    'character_avatar',
    OLD.id,
    'DELETE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'account_id', OLD.account_id,
        'chapter_id', OLD.chapter_id,
        'scene_id', OLD.scene_id,
        'race_detail_id', OLD.race_detail_id,
        'name', OLD.name,
        'flag', OLD.flag
    )
);
END;

DROP TRIGGER IF EXISTS `audit_chapter_after_insert`;
CREATE TRIGGER `audit_chapter_after_insert` AFTER INSERT ON `chapter` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, new_data)
VALUES (
    'chapter',
    NEW.id,
    'INSERT',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'starting_scene_id', NEW.starting_scene_id
    )
);
END;

DROP TRIGGER IF EXISTS `audit_chapter_after_update`;
CREATE TRIGGER `audit_chapter_after_update` AFTER UPDATE ON `chapter` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data, new_data)
VALUES (
    'chapter',
    NEW.id,
    'UPDATE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'starting_scene_id', OLD.starting_scene_id
    ),
    JSON_OBJECT(
        'id', NEW.id,
        'name', NEW.name,
        'starting_scene_id', NEW.starting_scene_id
    )
);
END;

DROP TRIGGER IF EXISTS `audit_chapter_after_delete`;
CREATE TRIGGER `audit_chapter_after_delete` AFTER DELETE ON `chapter` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data)
VALUES (
    'chapter',
    OLD.id,
    'DELETE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'name', OLD.name,
        'starting_scene_id', OLD.starting_scene_id
    )
);
END;

DROP TRIGGER IF EXISTS `audit_scene_after_insert`;
CREATE TRIGGER `audit_scene_after_insert` AFTER INSERT ON `scene` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, new_data)
VALUES (
    'scene',
    NEW.id,
    'INSERT',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', NEW.id,
        'chapter_id', NEW.chapter_id,
        'name', NEW.name
    )
);
END;

DROP TRIGGER IF EXISTS `audit_scene_after_update`;
CREATE TRIGGER `audit_scene_after_update` AFTER UPDATE ON `scene` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data, new_data)
VALUES (
    'scene',
    NEW.id,
    'UPDATE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'chapter_id', OLD.chapter_id,
        'name', OLD.name
    ),
    JSON_OBJECT(
        'id', NEW.id,
        'chapter_id', NEW.chapter_id,
        'name', NEW.name
    )
);
END;

DROP TRIGGER IF EXISTS `audit_scene_after_delete`;
CREATE TRIGGER `audit_scene_after_delete` AFTER DELETE ON `scene` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data)
VALUES (
    'scene',
    OLD.id,
    'DELETE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'chapter_id', OLD.chapter_id,
        'name', OLD.name
    )
);
END;

DROP TRIGGER IF EXISTS `create_character_related_tables`;
CREATE TRIGGER `create_character_related_tables` AFTER INSERT ON `character_avatar` FOR EACH ROW
BEGIN
INSERT INTO inventory (character_id)
VALUES (NEW.id);

INSERT INTO equipment (character_id)
VALUES (NEW.id);

INSERT INTO character_details (character_id, intelligence, charisma, fashion)
VALUES (NEW.id, 5, 5, 5);

INSERT INTO character_path (character_id)
VALUES (NEW.id);

END;



DELETE FROM character_path_choice;
DELETE FROM character_has_quest;
DELETE FROM inventory_has_item;
DELETE FROM choice_has_item;
DELETE FROM quest_has_item;
DELETE FROM scene_has_npc;
DELETE FROM character_path;
DELETE FROM inventory;
DELETE FROM character_details;
DELETE FROM equipment;
DELETE FROM character_avatar;
DELETE FROM choice;
DELETE FROM quest;
DELETE FROM npc;
DELETE FROM item;
DELETE FROM scene;
DELETE FROM chapter;
DELETE FROM account;
DELETE FROM race_details;

ALTER TABLE account AUTO_INCREMENT = 1;
ALTER TABLE chapter AUTO_INCREMENT = 1;
ALTER TABLE race_details AUTO_INCREMENT = 1;
ALTER TABLE scene AUTO_INCREMENT = 1;
ALTER TABLE character_avatar AUTO_INCREMENT = 1;
ALTER TABLE quest AUTO_INCREMENT = 1;
ALTER TABLE character_path AUTO_INCREMENT = 1;
ALTER TABLE choice AUTO_INCREMENT = 1;
ALTER TABLE item AUTO_INCREMENT = 1;
ALTER TABLE inventory AUTO_INCREMENT = 1;
ALTER TABLE npc AUTO_INCREMENT = 1;

INSERT INTO account (id, username, character_limit, email, password, role) VALUES
    (1, 'astra', 3, 'astra@chooseyourfate.dk', '$2b$10$Ruvc2k.FR5tK3GDvs2wU5OuIxZ/MBpvdCt.yPyYcQ9IQ5VVMX/08C', 'ROLE_USER'),
    (2, 'bjorn', 3, 'bjorn@chooseyourfate.dk', '$2b$10$JTD7.poDApqumMdqEb.hlODpLI/QVx2T1U7EYlE8prUm6XpabmU1G', 'ROLE_USER'),
    (3, 'cora', 9999, 'cora@chooseyourfate.dk', '$2b$10$iZ9YNxMMbTNypAe9bbc9iO2Gx6Vry//6EvkvaqzoaOQeZkslRQl42', 'ROLE_USER'),
    (4, 'admin', 9999, 'admin@chooseyourfate.dk', '$2b$10$oRncOoF8dWCtNtBanr/NxOVzSrK5ZxyHZthp9CIzo44W3fKgS..sK', 'ROLE_ADMIN'),
    (5, 'fisk', 9999, 'fisk@chooseyourfate.dk', '$2a$10$5nGOtIa9Qo1ptR3Z1dUhue1uJJAiifw.NAo0nUpWE9PCKG3zly0Yi', 'ROLE_USER');


INSERT INTO chapter (id, name, starting_scene_id) VALUES
    (1, 'The Festival Gate', NULL),
    (2, 'Moonlit Ruins', NULL),
    (3, 'bobs playground', NULL),
    (4, 'Tall forest, small people', NULL),
    (5, 'Small forest, tall people', NULL);

INSERT INTO race_details (id, name, starting_chapter_id) VALUES
    (1, 'Bobs', 3),
    (2, 'Bobiticus laviticus', 3),
    (3, 'smalls', 4),
    (4, 'TALLS', 5),
    (5, 'MeDiUmS', 2);

INSERT INTO scene (id, chapter_id, name) VALUES
    (1, 1, 'Town Gate'),
    (2, 2, 'Market Square'),
    (3, 3, 'Watchtower'),
    (4, 4, 'Forest Path'),
    (5, 5, 'Moon Shrine');

INSERT INTO item (id, name, description, type) VALUES
    (1, 'Rusty Sword', 'Old sword, still sharp enough for fights.', 'weapon'),
    (2, 'Bronze Helmet', 'Dented helmet from the city guard.', 'armor_head'),
    (3, 'Leather Jerkin', 'Light armor favored by scouts.', 'armor_chest'),
    (4, 'Traveler Pants', 'Sturdy pants with useful pockets.', 'armor_legs'),
    (5, 'Healing Potion', 'Restores strength and confidence.', 'consumable'),
    (6, 'Moon Key', 'Silver key etched with moon symbols.', 'quest'),
    (7, 'Festival Token', 'Brass token used for festival entry.', 'quest'),
    (8, 'Iron Helmet', 'Sturdy helmet forged by a city blacksmith.', 'armor_head'),
    (9, 'Chain Vest', 'Interlocked rings offer decent protection.', 'armor_chest'),
    (10, 'Iron Greaves', 'Heavy leg armor slowing your movement slightly.', 'armor_legs');

INSERT INTO npc (id, name, race_details_id) VALUES
    (1, 'Captain Elira', 1),
    (2, 'Merchant Vonn', 2),
    (3, 'Archivist Nyx', 3);

INSERT INTO scene_has_npc (scene_id, npc_id) VALUES
    (1, 1),
    (2, 2),
    (5, 3);

INSERT INTO quest (id, scene_id, description) VALUES
    (1, 1, 'Earn a festival token by helping the guard inspect travelers at the gate.'),
    (2, 2, 'Find the hidden moon key before the shrine doors are sealed at dusk.'),
    (3, 5, 'Return the moon key to Archivist Nyx and unlock the shrine archive.');

INSERT INTO quest_has_item (quest_id, item_id) VALUES
    (1, 7),
    (2, 6),
    (3, 5);

INSERT INTO choice (id, scene_id, destination_scene_id, description, consequence, target_id, value_int, story_weight, requirements) VALUES
    (1, 1, 2, 'Show the guard your invitation and step into the festival crowd.', 'gain_quest', 1, 1, 8, '{"requires":[],"grants":["festival-access"]}'),
    (2, 1, 3, 'Climb the watchtower to get a better view of the city.', 'gain_stat', NULL, 1, 5, '{"requires":[],"grants":["watchtower-visited"]}'),
    (3, 2, 4, 'Follow the suspicious footprints leading away from the market.', 'travel', 2, 0, 9, '{"requires":["festival-access"],"grants":["forest-route"]}'),
    (4, 3, 4, 'Leap down from the tower and cut through the forest trail.', 'lose_hp', NULL, -1, 7, '{"requires":["watchtower-visited"],"grants":["bruised"]}'),
    (5, 4, 5, 'Use the moon key to open the shrine gate.', 'complete_quest', 3, 1, 10, '{"requires":["item:6"],"grants":["shrine-open"]}'),
    (6, 2, 1, 'Return to the gate and report what you learned.', 'travel', NULL, 0, 4, '{"requires":[],"grants":[]}');

INSERT INTO choice_has_item (choice_id, item_id) VALUES
    (3, 6),
    (5, 5);

INSERT INTO character_avatar (id, account_id, chapter_id, scene_id, race_detail_id, name, flag) VALUES
    (1, 1, 1, 1, 1, 'Lyra', '{"reputation":{"guard":1},"statusEffects":[],"storyFlags":["festival-access"]}'),
    (2, 2, 1, 1, 2, 'Torben', '{"reputation":{"market":0},"statusEffects":[],"storyFlags":["watchtower-visited"]}'),
    (3, 3, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}'),
    (4, 4, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}'),
    (5, 4, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}'),
    (6, 4, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}'),
    (7, 4, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}');


UPDATE character_details
SET intelligence = 7, charisma = 6, fashion = 5
WHERE character_id = 1;

UPDATE character_details
SET intelligence = 4, charisma = 5, fashion = 7
WHERE character_id = 2;

UPDATE character_details
SET intelligence = 9, charisma = 8, fashion = 6
WHERE character_id = 3;

UPDATE character_path
SET summary = 'YOYOY' -- Should not be longer as it would use too many tokens for the tests.
--SET audio_blob = NULL
, summary_updated_at = "2026-05-30T22:27:06"
--SET audio_blob_updated_at = NULL
WHERE character_id = 1;

UPDATE character_path
SET summary = '' --Has to be empty for a test in Text to speech
--SET audio_blob = NULL
--SET summary_updated_at = NULL
--SET audio_blob_updated_at = NULL
WHERE character_id = 2;

UPDATE character_path
SET summary = NULL --Has to be null for a test in Text to speech
--SET audio_blob = NULL
--SET summary_updated_at = NULL
--SET audio_blob_updated_at = NULL
WHERE character_id = 3;

UPDATE character_path --outdated blob
SET summary = 'YOYOY' -- Should not be longer as it would use too many tokens for the tests.
, audio_blob = X'48656C6C6F' --false blob data it is a byte array, but it is not an audio file 
, summary_updated_at = "2026-05-30T22:27:06"
, audio_blob_updated_at = "2026-05-29T22:27:06"
WHERE character_id = 4;

UPDATE character_path --up to date blob
SET summary = 'YOYOY' -- Should not be longer as it would use too many tokens for the tests.
, audio_blob = X'48656C6C6F' --false blob data it is a byte array, but it is not an audio file 
, summary_updated_at = "2026-05-30T22:27:06"
, audio_blob_updated_at = "2026-05-30T22:28:06"
WHERE character_id = 5;

UPDATE character_path --up to date blob and no summary
SET summary = '' -- Should not be longer as it would use too many tokens for the tests.
, audio_blob = X'48656C6C6F' --false blob data it is a byte array, but it is not an audio file 
--, summary_updated_at = "2026-05-30T22:27:06"
, audio_blob_updated_at = "2026-05-30T22:27:06"
WHERE character_id = 6;

UPDATE character_path --up to date blob and no summary
SET summary = 'YOYOY' -- Should not be longer as it would use too many tokens for the tests.
, audio_blob = X'48656C6C6F' --false blob data it is a byte array, but it is not an audio file 
, summary_updated_at = "2026-05-30T22:27:06"
, audio_blob_updated_at = "2026-05-29T22:27:06"
WHERE character_id = 7;

INSERT INTO character_has_quest (character_id, quest_id, status) VALUES
    (1, 1, 0),
    (1, 2, 0),
    (2, 1, 0),
    (3, 2, 0),
    (3, 3, 0);

UPDATE character_has_quest
SET status = 1
WHERE character_id = 1 AND quest_id = 1;

UPDATE character_has_quest
SET status = 1
WHERE character_id = 3 AND quest_id = 2;

UPDATE character_has_quest
SET status = 1
WHERE character_id = 3 AND quest_id = 3;


UPDATE equipment
SET head = 2, chest = 3, legs = 4
WHERE character_id = 3;

UPDATE chapter
SET starting_scene_id = 1
WHERE id = 1;

UPDATE chapter
SET starting_scene_id = 2
WHERE id = 2;

UPDATE chapter
SET starting_scene_id = 3
WHERE id = 3;

UPDATE chapter
SET starting_scene_id = 4
WHERE id = 4;

UPDATE chapter
SET starting_scene_id = 5
WHERE id = 5;

CALL sp_make_choice(1, 1);
CALL sp_make_choice(1, 3);
CALL sp_make_choice(2, 2);

-- Supplemental mock data seed.
-- Assumes 05_seed_data.sql has already been executed and extends the dataset to 100 rows
-- for the core tables while keeping the data connected and reasonably realistic.

DROP TEMPORARY TABLE IF EXISTS tmp_seq_100;
CREATE TEMPORARY TABLE tmp_seq_100 (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_seq_100 (n)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM seq
    WHERE n < 100
)
SELECT n
FROM seq;

-- ---------------------------------------------------------------------------
-- Accounts (5 existing -> add 96)
-- ---------------------------------------------------------------------------
INSERT INTO account (id, username, character_limit, email, password, role)
SELECT
    n,
    CONCAT('user', LPAD(n, 3, '0')),
    3 + MOD(n, 3),
    CONCAT('user', LPAD(n, 3, '0'), '@chooseyourfate.dk'),
    '$2b$10$Ruvc2k.FR5tK3GDvs2wU5OuIxZ/MBpvdCt.yPyYcQ9IQ5VVMX/08C',
    CASE
        WHEN MOD(n, 25) = 0 THEN 'ROLE_ADMIN'
        ELSE 'ROLE_USER'
    END
FROM tmp_seq_100
WHERE n BETWEEN 6 AND 100;

-- ---------------------------------------------------------------------------
-- Chapters (5 existing -> add 95)
-- ---------------------------------------------------------------------------
INSERT INTO chapter (id, name, starting_scene_id)
SELECT
    n,
    CONCAT(
        ELT(MOD(n - 1, 8) + 1,
            'Ashen', 'Moonlit', 'Verdant', 'Ivory',
            'Crimson', 'Storm', 'Ember', 'Silver'
        ),
        ' Chapter ',
        LPAD(n, 3, '0')
    ),
    NULL
FROM tmp_seq_100
WHERE n BETWEEN 6 AND 100;

-- ---------------------------------------------------------------------------
-- Race details (5 existing -> add 95)
-- ---------------------------------------------------------------------------
INSERT INTO race_details (id, name, starting_chapter_id)
SELECT
    n,
    CONCAT(
        ELT(MOD(n - 1, 6) + 1,
            'Highland', 'Riverborn', 'Duskwind',
            'Sunforge', 'Mistfolk', 'Starling'
        ),
        ' lineage ',
        LPAD(n, 3, '0')
    ),
    n
FROM tmp_seq_100
WHERE n BETWEEN 6 AND 100;

-- ---------------------------------------------------------------------------
-- Scenes (5 existing -> add 95)
-- ---------------------------------------------------------------------------
INSERT INTO scene (id, chapter_id, name)
SELECT
    n,
    n,
    CONCAT(
        ELT(MOD(n - 1, 8) + 1,
            'Outer Gate', 'Lantern Market', 'North Tower', 'Moss Trail',
            'Shrine Court', 'Dockside Alley', 'Hidden Archive', 'Hill Camp'
        ),
        ' ',
        LPAD(n, 3, '0')
    )
FROM tmp_seq_100
WHERE n BETWEEN 6 AND 100;

UPDATE chapter
SET starting_scene_id = id
WHERE id BETWEEN 6 AND 100;

-- ---------------------------------------------------------------------------
-- Items (10 existing -> add 90)
-- Cycle: head, chest, legs, weapon, consumable, quest
-- ---------------------------------------------------------------------------
INSERT INTO item (id, name, description, type)
SELECT
    n,
    CASE MOD(n - 11, 6)
        WHEN 0 THEN CONCAT('Sentinel Helm ', LPAD(n, 3, '0'))
        WHEN 1 THEN CONCAT('Brigandine Vest ', LPAD(n, 3, '0'))
        WHEN 2 THEN CONCAT('Trail Greaves ', LPAD(n, 3, '0'))
        WHEN 3 THEN CONCAT('Wayfarer Blade ', LPAD(n, 3, '0'))
        WHEN 4 THEN CONCAT('Tonic Flask ', LPAD(n, 3, '0'))
        ELSE CONCAT('Seal Fragment ', LPAD(n, 3, '0'))
    END,
    CASE MOD(n - 11, 6)
        WHEN 0 THEN 'A reinforced helmet issued to city sentries on remote patrol routes.'
        WHEN 1 THEN 'Layered chest armor built for couriers and scouts moving between districts.'
        WHEN 2 THEN 'Reliable leg protection worn by marsh guides and shrine wardens.'
        WHEN 3 THEN 'A serviceable blade balanced for travelers expecting trouble on the road.'
        WHEN 4 THEN 'A restorative tonic brewed for long patrols and harsh weather.'
        ELSE 'A stamped fragment used to prove passage, rank, or ritual clearance.'
    END,
    CASE MOD(n - 11, 6)
        WHEN 0 THEN 'armor_head'
        WHEN 1 THEN 'armor_chest'
        WHEN 2 THEN 'armor_legs'
        WHEN 3 THEN 'weapon'
        WHEN 4 THEN 'consumable'
        ELSE 'quest'
    END
FROM tmp_seq_100
WHERE n BETWEEN 11 AND 100;

-- ---------------------------------------------------------------------------
-- NPCs (3 existing -> add 97)
-- ---------------------------------------------------------------------------
INSERT INTO npc (id, name, race_details_id)
SELECT
    n,
    CONCAT(
        ELT(MOD(n - 1, 8) + 1,
            'Captain', 'Merchant', 'Archivist', 'Scout',
            'Caretaker', 'Hunter', 'Guide', 'Warden'
        ),
        ' ',
        ELT(MOD(n - 1, 10) + 1,
            'Rowan', 'Selene', 'Bram', 'Iris', 'Tovin',
            'Nyra', 'Cael', 'Mira', 'Sorrel', 'Varr'
        ),
        ' ',
        LPAD(n, 3, '0')
    ),
    n
FROM tmp_seq_100
WHERE n BETWEEN 4 AND 100;

INSERT INTO scene_has_npc (scene_id, npc_id)
SELECT
    n,
    n
FROM tmp_seq_100
WHERE n BETWEEN 4 AND 100;

-- ---------------------------------------------------------------------------
-- Quests (3 existing -> add 97)
-- ---------------------------------------------------------------------------
INSERT INTO quest (id, scene_id, description)
SELECT
    n,
    n,
    CONCAT(
        'Support the locals in scene ',
        LPAD(n, 3, '0'),
        ' by securing a clue, completing a delivery, or exposing a nearby threat.'
    )
FROM tmp_seq_100
WHERE n BETWEEN 4 AND 100;

INSERT INTO quest_has_item (quest_id, item_id)
SELECT
    n,
    11 + MOD(n - 4, 90)
FROM tmp_seq_100
WHERE n BETWEEN 4 AND 100;

-- ---------------------------------------------------------------------------
-- Choices (6 existing -> add 94)
-- Scene coverage becomes 1..98, which gives every generated character a valid first choice.
-- ---------------------------------------------------------------------------
INSERT INTO choice (
    id,
    scene_id,
    destination_scene_id,
    description,
    consequence,
    target_id,
    value_int,
    story_weight,
    requirements
)
SELECT
    n,
    n - 2,
    n - 1,
    CASE MOD(n - 7, 5)
        WHEN 0 THEN CONCAT('Press onward from scene ', LPAD(n - 2, 3, '0'), ' and follow the clearest road toward the next district.')
        WHEN 1 THEN CONCAT('Speak with the local authority in scene ', LPAD(n - 2, 3, '0'), ' and accept a structured task.')
        WHEN 2 THEN CONCAT('Search the area around scene ', LPAD(n - 2, 3, '0'), ' for tactical advantages before moving on.')
        WHEN 3 THEN CONCAT('Take the risky shortcut out of scene ', LPAD(n - 2, 3, '0'), ' to save time at a personal cost.')
        ELSE CONCAT('Use what you have learned in scene ', LPAD(n - 2, 3, '0'), ' to close out the current objective.')
    END,
    CASE MOD(n - 7, 5)
        WHEN 0 THEN 'travel'
        WHEN 1 THEN 'gain_quest'
        WHEN 2 THEN 'gain_stat'
        WHEN 3 THEN 'lose_hp'
        ELSE 'complete_quest'
    END,
    CASE MOD(n - 7, 5)
        WHEN 1 THEN n - 2
        WHEN 4 THEN n - 2
        ELSE NULL
    END,
    CASE MOD(n - 7, 5)
        WHEN 0 THEN 0
        WHEN 1 THEN 1
        WHEN 2 THEN 2
        WHEN 3 THEN -1
        ELSE 1
    END,
    4 + MOD(n, 7),
    CASE MOD(n - 7, 5)
        WHEN 0 THEN JSON_OBJECT('requires', JSON_ARRAY(), 'grants', JSON_ARRAY(CONCAT('road-', LPAD(n - 1, 3, '0'))))
        WHEN 1 THEN JSON_OBJECT('requires', JSON_ARRAY(), 'grants', JSON_ARRAY(CONCAT('quest-', LPAD(n - 2, 3, '0'))))
        WHEN 2 THEN JSON_OBJECT('requires', JSON_ARRAY(), 'grants', JSON_ARRAY('prepared'))
        WHEN 3 THEN JSON_OBJECT('requires', JSON_ARRAY('prepared'), 'grants', JSON_ARRAY('bruised'))
        ELSE JSON_OBJECT('requires', JSON_ARRAY(CONCAT('quest-', LPAD(n - 2, 3, '0'))), 'grants', JSON_ARRAY('resolved'))
    END
FROM tmp_seq_100
WHERE n BETWEEN 7 AND 100;

INSERT INTO choice_has_item (choice_id, item_id)
SELECT
    seeded.choice_id,
    seeded.item_id
FROM (
    SELECT 1 AS choice_id, 7 AS item_id
    UNION ALL SELECT 2, 5
    UNION ALL SELECT 4, 5
    UNION ALL SELECT 6, 7
    UNION ALL
    SELECT n, 11 + MOD(n - 7, 90)
    FROM tmp_seq_100
    WHERE n BETWEEN 7 AND 100
) AS seeded;

-- ---------------------------------------------------------------------------
-- Characters (3 existing -> add 97)
-- Trigger auto-creates inventory, equipment, character_details and character_path.
-- Every generated character starts in a scene that has at least one choice.
-- ---------------------------------------------------------------------------
INSERT INTO character_avatar (
    id,
    account_id,
    chapter_id,
    scene_id,
    race_detail_id,
    name,
    flag
)
SELECT
    n,
    n,
    ((n - 4) % 98) + 1,
    ((n - 4) % 98) + 1,
    ((n - 4) % 100) + 1,
    CONCAT(
        ELT(MOD(n - 1, 10) + 1,
            'Aela', 'Borin', 'Cira', 'Dain', 'Eris',
            'Fenn', 'Galen', 'Hana', 'Ivor', 'Jora'
        ),
        ' ',
        ELT(MOD(n + 2, 10) + 1,
            'Ash', 'Vale', 'Thorn', 'Kestrel', 'Rune',
            'Morrow', 'Pike', 'Dawn', 'Frost', 'Quill'
        ),
        ' ',
        LPAD(n, 3, '0')
    ),
    JSON_OBJECT(
        'reputation', JSON_OBJECT(
            'guard', MOD(n, 5) - 2,
            'market', MOD(n + 1, 5) - 2,
            'archive', MOD(n + 2, 5) - 2
        ),
        'statusEffects', JSON_ARRAY(),
        'storyFlags', JSON_ARRAY(CONCAT('origin-', LPAD(((n - 4) % 98) + 1, 3, '0')))
    )
FROM tmp_seq_100
WHERE n BETWEEN 8 AND 100;

UPDATE character_details cd
JOIN character_avatar ca ON ca.id = cd.character_id
SET
    cd.intelligence = 3 + MOD(ca.id, 8),
    cd.charisma = 3 + MOD(ca.id + 2, 8),
    cd.fashion = 3 + MOD(ca.id + 4, 8)
WHERE ca.id BETWEEN 4 AND 100;

UPDATE equipment e
SET
    e.head = CASE
        WHEN MOD(e.character_id, 3) = 0 THEN NULL
        ELSE 11 + (6 * MOD(e.character_id - 4, 15))
    END,
    e.chest = CASE
        WHEN MOD(e.character_id, 4) = 0 THEN NULL
        ELSE 12 + (6 * MOD(e.character_id - 4, 15))
    END,
    e.legs = CASE
        WHEN MOD(e.character_id, 5) = 0 THEN NULL
        ELSE 13 + (6 * MOD(e.character_id - 4, 15))
    END
WHERE e.character_id BETWEEN 4 AND 100;

INSERT INTO character_has_quest (character_id, quest_id, status)
SELECT
    n,
    n,
    CASE
        WHEN MOD(n, 4) = 0 THEN 1
        ELSE 0
    END
FROM tmp_seq_100
WHERE n BETWEEN 4 AND 98;

INSERT INTO inventory_has_item (item_id, inventory_id, amount)
SELECT
    11 + MOD(i.character_id - 4, 90),
    i.id,
    1 + MOD(i.character_id, 3)
FROM inventory i
WHERE i.character_id BETWEEN 4 AND 95;

INSERT INTO character_path_choice (character_path_id, choice_id)
SELECT
    cp.id,
    scene_choices.choice_id
FROM character_path cp
JOIN character_avatar ca
    ON ca.id = cp.character_id
JOIN (
    SELECT scene_id, MIN(id) AS choice_id
    FROM choice
    GROUP BY scene_id
) AS scene_choices
    ON scene_choices.scene_id = ca.scene_id
WHERE ca.id BETWEEN 4 AND 100;

UPDATE character_avatar ca
JOIN character_path cp
    ON cp.character_id = ca.id
JOIN character_path_choice cpc
    ON cpc.character_path_id = cp.id
JOIN choice ch
    ON ch.id = cpc.choice_id
JOIN scene dest
    ON dest.id = ch.destination_scene_id
SET
    ca.scene_id = dest.id,
    ca.chapter_id = dest.chapter_id
WHERE ca.id BETWEEN 4 AND 100;

UPDATE character_path cp
JOIN character_avatar ca
    ON ca.id = cp.character_id
SET cp.summary = CONCAT(
    ca.name,
    ' has already moved beyond their starting point and is currently navigating scene ',
    LPAD(ca.scene_id, 3, '0'),
    ' while balancing faction ties and personal survival.'
)
WHERE ca.id BETWEEN 8 AND 100;

DROP TEMPORARY TABLE IF EXISTS tmp_seq_100;


-- Expands the narrative part of the dataset so chapters can contain multiple scenes
-- and scenes can branch through several choices. Intended to run after:
-- 01_create_schema.sql
-- 02_procedures.sql
-- 03_functions.sql
-- 04_triggers.sql
-- 05_seed_data.sql
-- 06_seed_mock_data.sql

DROP TEMPORARY TABLE IF EXISTS tmp_extra_scene_seq;
CREATE TEMPORARY TABLE tmp_extra_scene_seq (
    seq INT NOT NULL PRIMARY KEY
);

INSERT INTO tmp_extra_scene_seq (seq)
WITH RECURSIVE seq_gen AS (
    SELECT 1 AS seq
    UNION ALL
    SELECT seq + 1
    FROM seq_gen
    WHERE seq < 100
)
SELECT seq
FROM seq_gen;

DROP TEMPORARY TABLE IF EXISTS tmp_extra_scenes;
CREATE TEMPORARY TABLE tmp_extra_scenes (
    seq INT NOT NULL PRIMARY KEY,
    scene_id INT NOT NULL,
    chapter_id INT NOT NULL,
    chapter_scene_no INT NOT NULL
);

INSERT INTO tmp_extra_scenes (seq, scene_id, chapter_id, chapter_scene_no)
SELECT
    seq,
    100 + seq AS scene_id,
    FLOOR((seq - 1) / 4) + 1 AS chapter_id,
    MOD(seq - 1, 4) + 1 AS chapter_scene_no
FROM tmp_extra_scene_seq;

-- ---------------------------------------------------------------------------
-- Add 100 extra scenes distributed across chapters 1-25 (4 extra scenes each).
-- ---------------------------------------------------------------------------
INSERT INTO scene (id, chapter_id, name)
SELECT
    scene_id,
    chapter_id,
    CONCAT(
        'Chapter ',
        LPAD(chapter_id, 3, '0'),
        ' - ',
        ELT(
            chapter_scene_no,
            'Approach Road',
            'Central Square',
            'Side Passage',
            'Final Crossing'
        )
    )
FROM tmp_extra_scenes;

-- ---------------------------------------------------------------------------
-- Add one extra quest per added scene.
-- ---------------------------------------------------------------------------
INSERT INTO quest (id, scene_id, description)
SELECT
    100 + seq,
    scene_id,
    CONCAT(
        'Resolve the local conflict in scene ',
        LPAD(scene_id, 3, '0'),
        ' by gathering leverage, speaking to the right people, and choosing who to support.'
    )
FROM tmp_extra_scenes;

INSERT INTO quest_has_item (quest_id, item_id)
SELECT
    100 + seq,
    11 + MOD(seq - 1, 90)
FROM tmp_extra_scenes;

-- ---------------------------------------------------------------------------
-- Add at least one NPC relation per added scene and a second one for half of them.
-- ---------------------------------------------------------------------------
INSERT INTO scene_has_npc (scene_id, npc_id)
SELECT
    scene_id,
    1 + MOD(seq - 1, 100)
FROM tmp_extra_scenes;

INSERT INTO scene_has_npc (scene_id, npc_id)
SELECT
    scene_id,
    1 + MOD(seq + 32, 100)
FROM tmp_extra_scenes
WHERE MOD(seq, 2) = 0;

-- ---------------------------------------------------------------------------
-- Add 3 choices per added scene:
-- 1. Continue deeper into the same chapter flow.
-- 2. Take on a local quest.
-- 3. Take a risky shortcut.
-- This adds 300 extra choices and gives scenes a more realistic branching shape.
-- Split into multiple INSERT statements to avoid MySQL's temp-table reopen limitation.
-- ---------------------------------------------------------------------------
INSERT INTO choice (
    id,
    scene_id,
    destination_scene_id,
    description,
    consequence,
    target_id,
    value_int,
    story_weight,
    requirements
)
SELECT
    100 + ((seq - 1) * 3) + 1 AS id,
    scene_id,
    CASE
        WHEN chapter_scene_no < 4 THEN scene_id + 1
        ELSE chapter_id + 1
    END AS destination_scene_id,
    CONCAT(
        'Push forward from ',
        'scene ',
        LPAD(scene_id, 3, '0'),
        ' and stay on the most direct route through the district.'
    ),
    'travel',
    NULL,
    0,
    7,
    JSON_OBJECT(
        'requires', JSON_ARRAY(),
        'grants', JSON_ARRAY(CONCAT('route-', LPAD(scene_id, 3, '0')))
    )
FROM tmp_extra_scenes;

INSERT INTO choice (
    id,
    scene_id,
    destination_scene_id,
    description,
    consequence,
    target_id,
    value_int,
    story_weight,
    requirements
)
SELECT
    100 + ((seq - 1) * 3) + 2 AS id,
    scene_id,
    CASE
        WHEN chapter_scene_no = 1 THEN scene_id + 2
        WHEN chapter_scene_no = 2 THEN scene_id + 1
        WHEN chapter_scene_no = 3 THEN scene_id + 1
        ELSE chapter_id + 1
    END AS destination_scene_id,
    CONCAT(
        'Speak with the locals in scene ',
        LPAD(scene_id, 3, '0'),
        ' and take responsibility for the trouble unfolding there.'
    ),
    'gain_quest',
    100 + seq,
    1,
    8,
    JSON_OBJECT(
        'requires', JSON_ARRAY(),
        'grants', JSON_ARRAY(CONCAT('quest-', LPAD(100 + seq, 3, '0')))
    )
FROM tmp_extra_scenes;

INSERT INTO choice (
    id,
    scene_id,
    destination_scene_id,
    description,
    consequence,
    target_id,
    value_int,
    story_weight,
    requirements
)
SELECT
    100 + ((seq - 1) * 3) + 3 AS id,
    scene_id,
    CASE
        WHEN chapter_scene_no IN (1, 2) THEN scene_id + 2
        WHEN chapter_scene_no = 3 THEN chapter_id + 1
        ELSE LEAST(scene_id, 200)
    END AS destination_scene_id,
    CONCAT(
        'Take a dangerous shortcut out of scene ',
        LPAD(scene_id, 3, '0'),
        ' and trade safety for speed.'
    ),
    CASE
        WHEN MOD(seq, 3) = 0 THEN 'lose_hp'
        WHEN MOD(seq, 3) = 1 THEN 'gain_stat'
        ELSE 'complete_quest'
    END,
    CASE
        WHEN MOD(seq, 3) = 2 THEN 100 + seq
        ELSE NULL
    END,
    CASE
        WHEN MOD(seq, 3) = 0 THEN -1
        WHEN MOD(seq, 3) = 1 THEN 2
        ELSE 1
    END,
    6,
    JSON_OBJECT(
        'requires', JSON_ARRAY(CONCAT('route-', LPAD(scene_id, 3, '0'))),
        'grants', JSON_ARRAY(
            CASE
                WHEN MOD(seq, 3) = 0 THEN 'bruised'
                WHEN MOD(seq, 3) = 1 THEN 'prepared'
                ELSE 'resolved'
            END
        )
    )
FROM tmp_extra_scenes;

-- ---------------------------------------------------------------------------
-- Some choices explicitly reference items to keep item-driven branching visible.
-- ---------------------------------------------------------------------------
INSERT INTO choice_has_item (choice_id, item_id)
SELECT
    100 + ((seq - 1) * 3) + 2,
    11 + MOD(seq - 1, 90)
FROM tmp_extra_scenes
WHERE MOD(seq, 2) = 1;

INSERT INTO choice_has_item (choice_id, item_id)
SELECT
    100 + ((seq - 1) * 3) + 3,
    11 + MOD(seq + 17, 90)
FROM tmp_extra_scenes
WHERE MOD(seq, 5) = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_extra_scenes;
DROP TEMPORARY TABLE IF EXISTS tmp_extra_scene_seq;

-- Spring security expects these exact values when using hasRole("USER") / hasRole("ADMIN").
UPDATE account
SET role = 'ROLE_USER'
WHERE role IS NULL
   OR role NOT IN ('ROLE_USER', 'ROLE_ADMIN');

ALTER TABLE account
    MODIFY role ENUM('ROLE_USER', 'ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER';

-- UPDATE account SET role = 'ROLE_ADMIN' WHERE username = 'your_admin_username';

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
