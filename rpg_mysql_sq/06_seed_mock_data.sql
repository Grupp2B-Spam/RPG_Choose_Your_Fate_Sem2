USE choose_your_fate;

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
INSERT INTO sceneId (id, chapter_id, name)
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
        'Support the locals in sceneId ',
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
        WHEN 0 THEN CONCAT('Press onward from sceneId ', LPAD(n - 2, 3, '0'), ' and follow the clearest road toward the next district.')
        WHEN 1 THEN CONCAT('Speak with the local authority in sceneId ', LPAD(n - 2, 3, '0'), ' and accept a structured task.')
        WHEN 2 THEN CONCAT('Search the area around sceneId ', LPAD(n - 2, 3, '0'), ' for tactical advantages before moving on.')
        WHEN 3 THEN CONCAT('Take the risky shortcut out of sceneId ', LPAD(n - 2, 3, '0'), ' to save time at a personal cost.')
        ELSE CONCAT('Use what you have learned in sceneId ', LPAD(n - 2, 3, '0'), ' to close out the current objective.')
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
-- Every generated character starts in a sceneId that has at least one choice.
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
WHERE n BETWEEN 4 AND 100;

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
JOIN sceneId dest
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
    ' has already moved beyond their starting point and is currently navigating sceneId ',
    LPAD(ca.scene_id, 3, '0'),
    ' while balancing faction ties and personal survival.'
)
WHERE ca.id BETWEEN 4 AND 100;

DROP TEMPORARY TABLE IF EXISTS tmp_seq_100;
