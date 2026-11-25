USE defaultdb;

SET FOREIGN_KEY_CHECKS = 0;

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
DELETE FROM sceneId;
DELETE FROM chapter;
DELETE FROM account;
DELETE FROM race_details;

ALTER TABLE account AUTO_INCREMENT = 1;
ALTER TABLE chapter AUTO_INCREMENT = 1;
ALTER TABLE race_details AUTO_INCREMENT = 1;
ALTER TABLE sceneId AUTO_INCREMENT = 1;
ALTER TABLE character_avatar AUTO_INCREMENT = 1;
ALTER TABLE quest AUTO_INCREMENT = 1;
ALTER TABLE character_path AUTO_INCREMENT = 1;
ALTER TABLE choice AUTO_INCREMENT = 1;
ALTER TABLE item AUTO_INCREMENT = 1;
ALTER TABLE inventory AUTO_INCREMENT = 1;
ALTER TABLE npc AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO account (id, username, character_limit, email, password, role) VALUES
    (1, 'astra', 3, 'astra@chooseyourfate.dk', '$2b$10$Ruvc2k.FR5tK3GDvs2wU5OuIxZ/MBpvdCt.yPyYcQ9IQ5VVMX/08C', 'ROLE_USER'),
    (2, 'bjorn', 3, 'bjorn@chooseyourfate.dk', '$2b$10$JTD7.poDApqumMdqEb.hlODpLI/QVx2T1U7EYlE8prUm6XpabmU1G', 'ROLE_USER'),
    (3, 'cora', 2, 'cora@chooseyourfate.dk', '$2b$10$iZ9YNxMMbTNypAe9bbc9iO2Gx6Vry//6EvkvaqzoaOQeZkslRQl42', 'ROLE_USER'),
    (4, 'admin', 5, 'admin@chooseyourfate.dk', '$2b$10$oRncOoF8dWCtNtBanr/NxOVzSrK5ZxyHZthp9CIzo44W3fKgS..sK', 'ROLE_ADMIN');

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

INSERT INTO sceneId (id, chapter_id, name) VALUES
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
    (3, 3, 5, 5, 3, 'Mira', '{"reputation":{"archive":2},"statusEffects":[],"storyFlags":["shrine-open"]}');

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
SET summary = 'Lyra entered through the gate and started uncovering the mystery behind the moon shrine.'
WHERE character_id = 1;

UPDATE character_path
SET summary = 'Torben prefers scouting from high ground before taking risks.'
WHERE character_id = 2;

UPDATE character_path
SET summary = 'Mira has already reached the shrine and is close to resolving the archive questline.'
WHERE character_id = 3;

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

INSERT INTO inventory_has_item (item_id, inventory_id, amount) VALUES
    (1, 1, 1),
    (5, 1, 2),
    (2, 2, 1),
    (3, 3, 1),
    (4, 3, 1),
    (8, 3, 1),
    (9, 3, 1),
    (10, 3, 1)
ON DUPLICATE KEY UPDATE amount = VALUES(amount);

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

