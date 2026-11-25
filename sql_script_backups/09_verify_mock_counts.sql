USE choose_your_fate;

-- Quick overview of row counts after running the base seed and mock seed.
SELECT 'account' AS table_name, COUNT(*) AS row_count FROM account
UNION ALL
SELECT 'chapter', COUNT(*) FROM chapter
UNION ALL
SELECT 'race_details', COUNT(*) FROM race_details
UNION ALL
SELECT 'sceneId', COUNT(*) FROM sceneId
UNION ALL
SELECT 'character_avatar', COUNT(*) FROM character_avatar
UNION ALL
SELECT 'character_details', COUNT(*) FROM character_details
UNION ALL
SELECT 'quest', COUNT(*) FROM quest
UNION ALL
SELECT 'character_has_quest', COUNT(*) FROM character_has_quest
UNION ALL
SELECT 'character_path', COUNT(*) FROM character_path
UNION ALL
SELECT 'choice', COUNT(*) FROM choice
UNION ALL
SELECT 'character_path_choice', COUNT(*) FROM character_path_choice
UNION ALL
SELECT 'item', COUNT(*) FROM item
UNION ALL
SELECT 'equipment', COUNT(*) FROM equipment
UNION ALL
SELECT 'inventory', COUNT(*) FROM inventory
UNION ALL
SELECT 'inventory_has_item', COUNT(*) FROM inventory_has_item
UNION ALL
SELECT 'npc', COUNT(*) FROM npc
UNION ALL
SELECT 'quest_has_item', COUNT(*) FROM quest_has_item
UNION ALL
SELECT 'scene_has_npc', COUNT(*) FROM scene_has_npc
UNION ALL
SELECT 'choice_has_item', COUNT(*) FROM choice_has_item
ORDER BY table_name;

-- Focus view for tables that are still below the 100-row target.
SELECT *
FROM (
    SELECT 'account' AS table_name, COUNT(*) AS row_count FROM account
    UNION ALL
    SELECT 'chapter', COUNT(*) FROM chapter
    UNION ALL
    SELECT 'race_details', COUNT(*) FROM race_details
    UNION ALL
    SELECT 'sceneId', COUNT(*) FROM sceneId
    UNION ALL
    SELECT 'character_avatar', COUNT(*) FROM character_avatar
    UNION ALL
    SELECT 'character_details', COUNT(*) FROM character_details
    UNION ALL
    SELECT 'quest', COUNT(*) FROM quest
    UNION ALL
    SELECT 'character_has_quest', COUNT(*) FROM character_has_quest
    UNION ALL
    SELECT 'character_path', COUNT(*) FROM character_path
    UNION ALL
    SELECT 'choice', COUNT(*) FROM choice
    UNION ALL
    SELECT 'character_path_choice', COUNT(*) FROM character_path_choice
    UNION ALL
    SELECT 'item', COUNT(*) FROM item
    UNION ALL
    SELECT 'equipment', COUNT(*) FROM equipment
    UNION ALL
    SELECT 'inventory', COUNT(*) FROM inventory
    UNION ALL
    SELECT 'inventory_has_item', COUNT(*) FROM inventory_has_item
    UNION ALL
    SELECT 'npc', COUNT(*) FROM npc
    UNION ALL
    SELECT 'quest_has_item', COUNT(*) FROM quest_has_item
    UNION ALL
    SELECT 'scene_has_npc', COUNT(*) FROM scene_has_npc
    UNION ALL
    SELECT 'choice_has_item', COUNT(*) FROM choice_has_item
) AS counts
WHERE row_count < 100
ORDER BY row_count ASC, table_name ASC;

-- Sanity checks for some of the more important relationships.
SELECT
    (SELECT COUNT(*) FROM character_avatar) AS characters,
    (SELECT COUNT(*) FROM character_details) AS character_details_rows,
    (SELECT COUNT(*) FROM equipment) AS equipment_rows,
    (SELECT COUNT(*) FROM inventory) AS inventory_rows,
    (SELECT COUNT(*) FROM character_path) AS character_path_rows;

SELECT COUNT(*) AS characters_with_invalid_scene_chapter
FROM character_avatar ca
JOIN sceneId s ON s.id = ca.scene_id
WHERE ca.chapter_id <> s.chapter_id;

SELECT COUNT(*) AS choices_with_missing_destination
FROM choice c
LEFT JOIN sceneId s ON s.id = c.destination_scene_id
WHERE c.destination_scene_id IS NOT NULL
  AND s.id IS NULL;
