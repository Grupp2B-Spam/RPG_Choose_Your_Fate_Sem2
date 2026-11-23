-- Development seed data.
--
-- Runs after 01_schema.sql, and only when the container initialises an empty data
-- directory — so it never overwrites authored content.
--
-- It is here rather than in a test fixture because two things need content to exist
-- before anyone has authored any: the scene-content demo in
-- `scenemanager-cqrs-og-skalering.md`, which generates a backlog by republishing scenes,
-- and anyone starting the service for the first time who wants to see a payload rather
-- than an empty list. The integration tests deliberately do NOT load this file; they
-- create exactly the content each test needs, so a change here cannot make a test pass or
-- fail for reasons the test does not state.
--
-- The content is carried over from the monolith's 05_seed_data.sql, with three
-- differences that are the point of the exercise:
--
--   * Every choice has a `beat` (ADR-007). The monolith has none, and they could not be
--     generated — each one here was written by reading what the choice does.
--   * `consequence` / `target_id` / `value_int` are expressed as typed effects (ADR-016).
--     Note what happened to choice 4: the monolith says `lose_hp`, and there is no hp in
--     either schema. It is seeded as a charisma penalty, which is a guess — the original
--     intent is not recoverable, which is precisely the cost that entry describes.
--   * `requirements` is no longer a JSON blob mixing story flags with "item:6" strings.

USE `story_content`;

-- Chapters first; starting scenes are set at the end, once the scenes exist.
INSERT INTO `chapter` (`id`, `name`) VALUES
  (1, 'The Festival Gate'),
  (2, 'Moonlit Ruins'),
  (3, 'Watchtower Heights'),
  (4, 'Tall forest, small people'),
  (5, 'Small forest, tall people');

INSERT INTO `scene` (`id`, `chapter_id`, `name`, `description`) VALUES
  (1, 1, 'Town Gate',
      'Banners snap above the gate. A guard waves travellers through, one at a time.'),
  (2, 2, 'Market Square',
      'Stalls crowd the square. Someone has left muddy footprints leading away north.'),
  (3, 3, 'Watchtower',
      'From up here the whole city is laid out, and so is the forest beyond it.'),
  (4, 4, 'Forest Path',
      'The trees lean close. The path is narrow enough that you walk it alone.'),
  (5, 5, 'Moon Shrine',
      'A silver door, shut. The carvings around it are a lock as much as a decoration.');

INSERT INTO `race` (`id`, `name`, `starting_chapter_id`) VALUES
  (1, 'Bobs', 3),
  (2, 'Bobiticus laviticus', 3),
  (3, 'smalls', 4),
  (4, 'TALLS', 5),
  (5, 'MeDiUmS', 2);

-- The monolith's `type` column mixed what a thing is with where it is worn. Split here:
-- ARMOR_* becomes EQUIPMENT plus a slot, WEAPON becomes EQUIPMENT in MAINHAND.
INSERT INTO `item` (`id`, `name`, `description`, `type`, `slot`) VALUES
  (1,  'Rusty Sword',     'Old sword, still sharp enough for fights.',   'EQUIPMENT',  'MAINHAND'),
  (2,  'Bronze Helmet',   'Dented helmet from the city guard.',          'EQUIPMENT',  'HEAD'),
  (3,  'Leather Jerkin',  'Light armor favored by scouts.',              'EQUIPMENT',  'CHEST'),
  (4,  'Traveler Pants',  'Sturdy pants with useful pockets.',           'EQUIPMENT',  'LEGS'),
  (5,  'Healing Potion',  'Restores strength and confidence.',           'CONSUMABLE', NULL),
  (6,  'Moon Key',        'Silver key etched with moon symbols.',        'QUEST',      NULL),
  (7,  'Festival Token',  'Brass token used for festival entry.',        'QUEST',      NULL),
  (8,  'Iron Helmet',     'Sturdy helmet forged by a city blacksmith.',  'EQUIPMENT',  'HEAD'),
  (9,  'Chain Vest',      'Interlocked rings offer decent protection.',  'EQUIPMENT',  'CHEST'),
  (10, 'Iron Greaves',    'Heavy leg armor slowing your movement.',      'EQUIPMENT',  'LEGS');

-- Neither requirements nor modifiers exist in the monolith's item table; the contract
-- publishes both, so they are authored here.
INSERT INTO `item_requirement` (`item_id`, `stat`, `amount`) VALUES
  (9, 'FASHION', 2),
  (10, 'FASHION', 2);

INSERT INTO `item_modifier` (`item_id`, `stat`, `amount`) VALUES
  (1, 'CHARISMA', 2),
  (3, 'FASHION', 1),
  (8, 'CHARISMA', 1),
  (9, 'CHARISMA', 2);

INSERT INTO `npc` (`id`, `name`, `race_id`) VALUES
  (1, 'Captain Elira',  1),
  (2, 'Merchant Vonn',  2),
  (3, 'Archivist Nyx',  3);

INSERT INTO `scene_npc` (`scene_id`, `npc_id`) VALUES
  (1, 1),
  (2, 2),
  (5, 3);

INSERT INTO `quest` (`id`, `scene_id`, `name`, `description`) VALUES
  (1, 1, 'The guard''s favour',
      'Earn a festival token by helping the guard inspect travelers at the gate.'),
  (2, 2, 'Before the doors close',
      'Find the hidden moon key before the shrine doors are sealed at dusk.'),
  (3, 5, 'The archive',
      'Return the moon key to Archivist Nyx and unlock the shrine archive.');

-- The reward an author promises. Its counterpart on the other side of the boundary is
-- inventory_has_item, which records what a player actually holds.
INSERT INTO `quest_reward_item` (`quest_id`, `item_id`) VALUES
  (1, 7),
  (2, 6),
  (3, 5);

-- Every beat below was written for this seed. There was nothing to copy: the monolith
-- stores what a choice *is*, never what taking it *did*, which is the gap ADR-007 closes.
INSERT INTO `choice` (`id`, `scene_id`, `destination_scene_id`, `label`, `beat`, `weight`, `ordinal`) VALUES
  (1, 1, 2, 'Show the guard your invitation and step into the festival crowd.',
      'You showed your invitation and were waved into the festival.', 8, 0),
  (2, 1, 3, 'Climb the watchtower to get a better view of the city.',
      'You climbed the watchtower and looked out over the city.', 5, 1),
  (3, 2, 4, 'Follow the suspicious footprints leading away from the market.',
      'You followed the footprints out of the market and into the trees.', 9, 0),
  (4, 3, 4, 'Leap down from the tower and cut through the forest trail.',
      'You jumped from the tower and limped away into the forest.', 7, 0),
  (5, 4, 5, 'Use the moon key to open the shrine gate.',
      'You turned the moon key and the shrine gate opened.', 10, 0),
  (6, 2, 1, 'Return to the gate and report what you learned.',
      'You went back to the gate and told the guard what you had found.', 4, 1);

-- The monolith kept these as JSON on the choice, mixing story flags with "item:6".
INSERT INTO `choice_requirement` (`choice_id`, `ordinal`, `kind`, `flag`, `item_id`) VALUES
  (3, 0, 'STORY_FLAG', 'festival-access',     NULL),
  (4, 0, 'STORY_FLAG', 'watchtower-visited',  NULL),
  (5, 0, 'ITEM',        NULL,                 6);

-- `travel` is not an effect kind: the destination is already destination_scene_id, and in
-- the monolith's data the two disagree.
INSERT INTO `choice_effect` (`choice_id`, `ordinal`, `kind`, `item_id`, `quest_id`, `stat`, `amount`, `flag`) VALUES
  (1, 0, 'GRANT_QUEST',    NULL, 1,    NULL,       NULL, NULL),
  (1, 1, 'GRANT_FLAG',     NULL, NULL, NULL,       NULL, 'festival-access'),
  (2, 0, 'GRANT_FLAG',     NULL, NULL, NULL,       NULL, 'watchtower-visited'),
  (3, 0, 'GRANT_ITEM',     6,    NULL, NULL,       NULL, NULL),
  (3, 1, 'GRANT_FLAG',     NULL, NULL, NULL,       NULL, 'forest-route'),
  -- The monolith says `lose_hp`. There is no hp. See the header.
  (4, 0, 'MODIFY_STAT',    NULL, NULL, 'CHARISMA', -1,   NULL),
  (4, 1, 'GRANT_FLAG',     NULL, NULL, NULL,       NULL, 'bruised'),
  (5, 0, 'COMPLETE_QUEST', NULL, 3,    NULL,       NULL, NULL),
  (5, 1, 'GRANT_ITEM',     5,    NULL, NULL,       NULL, NULL),
  (5, 2, 'GRANT_FLAG',     NULL, NULL, NULL,       NULL, 'shrine-open');

-- Last, now that the scenes exist. This column has no foreign key on purpose: chapter and
-- scene reference each other, and a circular constraint can only be satisfied by turning
-- the checks off.
UPDATE `chapter` SET `starting_scene_id` = 1 WHERE `id` = 1;
UPDATE `chapter` SET `starting_scene_id` = 2 WHERE `id` = 2;
UPDATE `chapter` SET `starting_scene_id` = 3 WHERE `id` = 3;
UPDATE `chapter` SET `starting_scene_id` = 4 WHERE `id` = 4;
UPDATE `chapter` SET `starting_scene_id` = 5 WHERE `id` = 5;
