USE defaultdb;

-- Expands the narrative part of the dataset so chapters can contain multiple scenes
-- and scenes can branch through several choices. Intended to run after:
-- 01_create_schema_aiven.sql
-- 02_procedures_aiven.sql
-- 03_functions_aiven.sql
-- 04_triggers_aiven.sql
-- 05_seed_data_aiven.sql
-- 06_seed_mock_data_aiven.sql

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

INSERT INTO sceneId (id, chapter_id, name)
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

INSERT INTO quest (id, scene_id, description)
SELECT
    100 + seq,
    scene_id,
    CONCAT(
        'Resolve the local conflict in sceneId ',
        LPAD(scene_id, 3, '0'),
        ' by gathering leverage, speaking to the right people, and choosing who to support.'
    )
FROM tmp_extra_scenes;

INSERT INTO quest_has_item (quest_id, item_id)
SELECT
    100 + seq,
    11 + MOD(seq - 1, 90)
FROM tmp_extra_scenes;

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
        'sceneId ',
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
        'Speak with the locals in sceneId ',
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
        'Take a dangerous shortcut out of sceneId ',
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
