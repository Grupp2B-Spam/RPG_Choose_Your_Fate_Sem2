USE defaultdb;

DELIMITER $$

DROP PROCEDURE IF EXISTS `sp_create_character`$$
CREATE PROCEDURE `sp_create_character`(
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
FROM sceneId
WHERE id = p_scene_id;

IF v_scene_chapter_id IS NULL THEN
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Selected sceneId does not exist.';
END IF;

IF v_scene_chapter_id != p_chapter_id THEN
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Selected sceneId does not belong to the selected chapter.';
END IF;

IF v_character_count < v_character_limit THEN
	INSERT INTO `character_avatar` (account_id, chapter_id, scene_id, race_detail_id, name, flag)
    VALUES (p_account_id, p_chapter_id, p_scene_id, p_race_detail_id, p_name, JSON_OBJECT());
    SET p_character_id = LAST_INSERT_ID();
ELSE
	SIGNAL SQLSTATE '45000'
		SET MESSAGE_TEXT = 'Character limit reached.';
END IF;

END$$

DROP PROCEDURE IF EXISTS `sp_delete_character`$$
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
END$$

DROP PROCEDURE IF EXISTS `sp_make_choice`$$
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
			SET MESSAGE_TEXT = 'Character is not in the correct sceneId for this choice.';
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
            FROM sceneId s
            JOIN choice c ON c.destination_scene_id = s.id
            WHERE c.id = p_choice_id
        )
    WHERE id = p_character_id;
    
    COMMIT;
END$$

DROP PROCEDURE IF EXISTS `sp_grant_quest_rewards`$$
CREATE PROCEDURE `sp_grant_quest_rewards`(
IN p_character_id INT,
IN p_quest_id INT)
BEGIN
INSERT INTO inventory_has_item (item_id, inventory_id, amount)
	SELECT quest_has_item.item_id, inventory.id, 1 FROM quest_has_item
    INNER JOIN inventory ON inventory.character_id = p_character_id
    WHERE quest_has_item.quest_id = p_quest_id
ON DUPLICATE KEY UPDATE amount = amount + 1;
END$$

DELIMITER ;
