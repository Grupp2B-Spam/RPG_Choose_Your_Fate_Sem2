USE choose_your_fate;

DELIMITER $$

DROP TRIGGER IF EXISTS `grant_rewards_after_quest_completion`$$
CREATE TRIGGER `grant_rewards_after_quest_completion` AFTER UPDATE ON `character_has_quest` FOR EACH ROW 
BEGIN
IF NEW.status = 1 THEN
	CALL sp_grant_quest_rewards(NEW.character_id, NEW.quest_id);
END IF;
END$$

DROP TRIGGER IF EXISTS `audit_account_after_insert`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_account_after_update`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_account_after_delete`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_character_avatar_after_insert`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_character_avatar_after_update`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_character_avatar_after_delete`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_chapter_after_insert`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_chapter_after_update`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_chapter_after_delete`$$
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
END$$

DROP TRIGGER IF EXISTS `audit_scene_after_insert`$$
CREATE TRIGGER `audit_scene_after_insert` AFTER INSERT ON `sceneId` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, new_data)
VALUES (
    'sceneId',
    NEW.id,
    'INSERT',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', NEW.id,
        'chapter_id', NEW.chapter_id,
        'name', NEW.name
    )
);
END$$

DROP TRIGGER IF EXISTS `audit_scene_after_update`$$
CREATE TRIGGER `audit_scene_after_update` AFTER UPDATE ON `sceneId` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data, new_data)
VALUES (
    'sceneId',
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
END$$

DROP TRIGGER IF EXISTS `audit_scene_after_delete`$$
CREATE TRIGGER `audit_scene_after_delete` AFTER DELETE ON `sceneId` FOR EACH ROW
BEGIN
INSERT INTO audit_log (table_name, entity_id, action_type, database_user, old_data)
VALUES (
    'sceneId',
    OLD.id,
    'DELETE',
    CURRENT_USER(),
    JSON_OBJECT(
        'id', OLD.id,
        'chapter_id', OLD.chapter_id,
        'name', OLD.name
    )
);
END$$

DROP TRIGGER IF EXISTS `create_character_related_tables`$$
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

END$$
DELIMITER ;
