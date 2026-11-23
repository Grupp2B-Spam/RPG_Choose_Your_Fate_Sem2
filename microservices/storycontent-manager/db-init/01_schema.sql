-- Schema for the story-content bounded context.
--
-- This is NOT a copy of the monolith's `choose_your_fate` schema. It holds the ten tables
-- that the content/state boundary assigned to this service, re-expressed for what this
-- service is for: authoring content and publishing it. See beslutningslog.md ADR-015.
--
-- Three differences from the monolith are worth pointing out, because they are decisions
-- rather than transcription errors:
--
--   1. Names follow the published contract rather than the old schema. `race_details`
--      is `race`, `scene_has_npc` is `scene_npc`. The AsyncAPI document says `raceId`
--      and is the vocabulary other teams are building against; having the table called
--      something else only adds a translation step inside this service.
--
--   2. `choice_has_item` is gone. A granted item is one kind of authored effect among
--      five, so it lives in `choice_effect` with the others instead of in a table that
--      can only express one of them. See ADR-016.
--
--   3. `consequence` / `target_id` / `value_int` are gone, replaced by typed effects, and
--      `requirements` is no longer free JSON. An untyped integer cannot be interpreted by
--      a consumer in another service, and the monolith's own data shows it was not
--      reliably interpreted inside one either. See ADR-016.
--
-- Everything a consumer can observe carries a version column (ADR-006). The aggregate
-- roots that are published are `scene`, `item` and `race`, so those are the three tables
-- that have one.
--
-- The container runs the files in db-init/ in filename order on an empty data directory.

CREATE DATABASE IF NOT EXISTS `story_content`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `story_content`;

-- ---------------------------------------------------------------------------
-- chapter
--
-- `starting_scene_id` is nullable and deliberately carries no foreign key. Chapter and
-- scene reference each other, and the monolith solved the cycle by making this column
-- nullable so a chapter can exist before its first scene does. The cycle is harmless here
-- because both tables are inside one service, but it still has to be expressible: a
-- chapter is created, then its scenes, then the starting scene is pointed at.
--
-- The foreign key is omitted rather than merely deferred because MySQL does not support
-- deferred constraints, so a circular FK pair can only be satisfied by inserting with
-- checks disabled. A constraint that has to be switched off to load data is not providing
-- the guarantee it appears to; the service validates this reference instead, where the
-- error message can say something useful.
-- ---------------------------------------------------------------------------
CREATE TABLE `chapter` (
  `id`                INT          NOT NULL AUTO_INCREMENT,
  `name`              VARCHAR(100) NOT NULL,
  `starting_scene_id` INT              NULL,
  `created_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_chapter_starting_scene` (`starting_scene_id`)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- scene — a published aggregate root
--
-- `version` is the `sceneVersion` in the contract. It is incremented on every change to
-- anything that appears in the scene's payload: the scene itself, any of its choices,
-- any choice's requirements or effects, which NPCs stand in it, and which quests it
-- offers. It is deliberately NOT incremented for changes a consumer cannot observe — for
-- example renaming an NPC, since only `npcIds` travels in the event.
--
-- `description` does not exist in the monolith at all. The contract publishes it, so the
-- prose shown to the player becomes something this service stores rather than something
-- that lives only in the frontend.
-- ---------------------------------------------------------------------------
CREATE TABLE `scene` (
  `id`          INT          NOT NULL AUTO_INCREMENT,
  `chapter_id`  INT          NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` MEDIUMTEXT       NULL,
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED',
  `version`     INT          NOT NULL DEFAULT 1,
  `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_scene_chapter` (`chapter_id`),
  CONSTRAINT `fk_scene_chapter`
    FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`),
  CONSTRAINT `ck_scene_status`
    CHECK (`status` IN ('PUBLISHED', 'RETIRED')),
  CONSTRAINT `ck_scene_version`
    CHECK (`version` >= 1)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- race — a published aggregate root
--
-- `race_details` in the monolith. Placed here rather than in CharacterManager because
-- `starting_chapter_id` is a mandatory reference into story content, and moving races out
-- would give CharacterManager a foreign key into this database (ADR-015).
--
-- `version` is bumped both when the race changes and when the starting scene of its
-- chapter changes, because `startingSceneId` is denormalised into the published payload
-- and the second changes the payload without changing the race (ADR-017).
-- ---------------------------------------------------------------------------
CREATE TABLE `race` (
  `id`                  INT          NOT NULL AUTO_INCREMENT,
  `name`                VARCHAR(100) NOT NULL,
  `starting_chapter_id` INT          NOT NULL,
  `status`              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `version`             INT          NOT NULL DEFAULT 1,
  `created_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_race_name` (`name`),
  KEY `ix_race_starting_chapter` (`starting_chapter_id`),
  CONSTRAINT `fk_race_starting_chapter`
    FOREIGN KEY (`starting_chapter_id`) REFERENCES `chapter` (`id`),
  CONSTRAINT `ck_race_status`
    CHECK (`status` IN ('ACTIVE', 'RETIRED')),
  CONSTRAINT `ck_race_version`
    CHECK (`version` >= 1)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- item — a published aggregate root
--
-- The monolith's `item.type` mixes what a thing is with where it is worn
-- (ARMOR_HEAD, WEAPON, CONSUMABLE...). The contract already separates the two, so this
-- schema follows the contract: `type` is EQUIPMENT / CONSUMABLE / QUEST, and `slot` is
-- set only for EQUIPMENT.
--
-- `slot`, `item_requirement` and `item_modifier` have no counterpart in the monolith. The
-- contract publishes all three, so this service has to be the place they are authored.
-- ---------------------------------------------------------------------------
CREATE TABLE `item` (
  `id`          INT          NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL,
  `description` TEXT             NULL,
  `type`        VARCHAR(16)  NOT NULL,
  `slot`        VARCHAR(16)      NULL,
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
  `version`     INT          NOT NULL DEFAULT 1,
  `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  CONSTRAINT `ck_item_type`
    CHECK (`type` IN ('EQUIPMENT', 'CONSUMABLE', 'QUEST')),
  CONSTRAINT `ck_item_slot`
    CHECK (`slot` IS NULL OR `slot` IN ('HEAD', 'CHEST', 'LEGS', 'MAINHAND')),
  -- A slot is meaningful only for equipment, and equipment without one cannot be worn.
  -- Expressed as a constraint because it is the kind of rule that is otherwise enforced
  -- in one code path and forgotten in the next.
  CONSTRAINT `ck_item_slot_only_for_equipment`
    CHECK ((`type` = 'EQUIPMENT' AND `slot` IS NOT NULL)
        OR (`type` <> 'EQUIPMENT' AND `slot` IS NULL)),
  CONSTRAINT `ck_item_status`
    CHECK (`status` IN ('ACTIVE', 'RETIRED')),
  CONSTRAINT `ck_item_version`
    CHECK (`version` >= 1)
) ENGINE = InnoDB;

-- The contract publishes `requirements` and `modifiers` as objects of statistic -> integer.
-- Stored as rows rather than JSON so that the statistic name is constrained: an authored
-- effect naming a statistic that does not exist is exactly the failure ADR-016 is about.
CREATE TABLE `item_requirement` (
  `item_id` INT         NOT NULL,
  `stat`    VARCHAR(16) NOT NULL,
  `amount`  INT         NOT NULL,
  PRIMARY KEY (`item_id`, `stat`),
  CONSTRAINT `fk_item_requirement_item`
    FOREIGN KEY (`item_id`) REFERENCES `item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_item_requirement_stat`
    CHECK (`stat` IN ('INTELLIGENCE', 'CHARISMA', 'FASHION'))
) ENGINE = InnoDB;

CREATE TABLE `item_modifier` (
  `item_id` INT         NOT NULL,
  `stat`    VARCHAR(16) NOT NULL,
  `amount`  INT         NOT NULL,
  PRIMARY KEY (`item_id`, `stat`),
  CONSTRAINT `fk_item_modifier_item`
    FOREIGN KEY (`item_id`) REFERENCES `item` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ck_item_modifier_stat`
    CHECK (`stat` IN ('INTELLIGENCE', 'CHARISMA', 'FASHION'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- npc
--
-- Content, and mandatorily dependent on `race`. This is the second independent reason
-- races belong to this service: even with no players at all, content already cannot be
-- created without them.
-- ---------------------------------------------------------------------------
CREATE TABLE `npc` (
  `id`         INT          NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(100) NOT NULL,
  `race_id`    INT          NOT NULL,
  `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_npc_race` (`race_id`),
  CONSTRAINT `fk_npc_race`
    FOREIGN KEY (`race_id`) REFERENCES `race` (`id`)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- quest
--
-- Authored, and `scene_id` is NOT NULL: a quest cannot exist without the scene that
-- offers it, which is what settles it as content rather than as the authored half of
-- `character_has_quest`.
-- ---------------------------------------------------------------------------
CREATE TABLE `quest` (
  `id`          INT          NOT NULL AUTO_INCREMENT,
  `scene_id`    INT          NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` MEDIUMTEXT       NULL,
  `created_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_quest_scene` (`scene_id`),
  CONSTRAINT `fk_quest_scene`
    FOREIGN KEY (`scene_id`) REFERENCES `scene` (`id`)
) ENGINE = InnoDB;

-- The reward an author promises for completing a quest. The counterpart on the state side
-- is `inventory_has_item`, which records what a player actually holds: the same shape on
-- opposite sides of the boundary, told apart by who wrote the row.
CREATE TABLE `quest_reward_item` (
  `quest_id` INT NOT NULL,
  `item_id`  INT NOT NULL,
  PRIMARY KEY (`quest_id`, `item_id`),
  KEY `ix_quest_reward_item_item` (`item_id`),
  CONSTRAINT `fk_quest_reward_item_quest`
    FOREIGN KEY (`quest_id`) REFERENCES `quest` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_quest_reward_item_item`
    FOREIGN KEY (`item_id`) REFERENCES `item` (`id`)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- scene_npc — which NPCs stand in which scene
-- ---------------------------------------------------------------------------
CREATE TABLE `scene_npc` (
  `scene_id` INT NOT NULL,
  `npc_id`   INT NOT NULL,
  PRIMARY KEY (`scene_id`, `npc_id`),
  KEY `ix_scene_npc_npc` (`npc_id`),
  CONSTRAINT `fk_scene_npc_scene`
    FOREIGN KEY (`scene_id`) REFERENCES `scene` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_scene_npc_npc`
    FOREIGN KEY (`npc_id`) REFERENCES `npc` (`id`)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- choice
--
-- Part of the scene aggregate: a choice is never published on its own, only inside its
-- scene, and editing one bumps that scene's version.
--
-- `beat` is NOT NULL, which is the whole point of ADR-007. It is one authored sentence
-- describing what happens if the choice is taken, copied into the player's path so that
-- the path records a fact rather than a pointer into content that can change underneath
-- it. Made mandatory at the schema level because it cannot be backfilled: reconstructing
-- it later would need to know what the scene said at the time, and that information no
-- longer exists. A nullable column here would quietly become a column that is null for
-- everything written before someone remembered.
--
-- `label` is what the player reads on the choice; the monolith called it `description`.
-- `ordinal` fixes presentation order, which the monolith left to insertion order via an
-- ORDER BY on the primary key.
-- ---------------------------------------------------------------------------
CREATE TABLE `choice` (
  `id`                   INT          NOT NULL AUTO_INCREMENT,
  `scene_id`             INT          NOT NULL,
  `destination_scene_id` INT          NOT NULL,
  `label`                VARCHAR(255) NOT NULL,
  `beat`                 VARCHAR(500) NOT NULL,
  `weight`               SMALLINT     NOT NULL DEFAULT 1,
  `ordinal`              INT          NOT NULL DEFAULT 0,
  `created_at`           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at`           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `ix_choice_scene` (`scene_id`),
  KEY `ix_choice_destination_scene` (`destination_scene_id`),
  CONSTRAINT `fk_choice_scene`
    FOREIGN KEY (`scene_id`) REFERENCES `scene` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_choice_destination_scene`
    FOREIGN KEY (`destination_scene_id`) REFERENCES `scene` (`id`),
  CONSTRAINT `ck_choice_beat_not_blank`
    CHECK (CHAR_LENGTH(TRIM(`beat`)) > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- choice_requirement — what must be true for a choice to be offered
--
-- The monolith kept this as JSON: {"requires": ["festival-access"], "grants": [...]},
-- mixed with entries like "item:6" — two vocabularies in one text field. Split into a
-- kind and a column per kind so that a requirement is either well-formed or rejected.
--
-- A STORY_FLAG's name is authored here but held on the state side, so the string is the
-- shared vocabulary between the two services. It travels verbatim in the event, which is
-- what lets the state side evaluate it without calling back.
-- ---------------------------------------------------------------------------
CREATE TABLE `choice_requirement` (
  `id`        INT         NOT NULL AUTO_INCREMENT,
  `choice_id` INT         NOT NULL,
  `ordinal`   INT         NOT NULL DEFAULT 0,
  `kind`      VARCHAR(16) NOT NULL,
  `flag`      VARCHAR(64)     NULL,
  `item_id`   INT             NULL,
  PRIMARY KEY (`id`),
  KEY `ix_choice_requirement_choice` (`choice_id`),
  KEY `ix_choice_requirement_item` (`item_id`),
  CONSTRAINT `fk_choice_requirement_choice`
    FOREIGN KEY (`choice_id`) REFERENCES `choice` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_choice_requirement_item`
    FOREIGN KEY (`item_id`) REFERENCES `item` (`id`),
  CONSTRAINT `ck_choice_requirement_kind`
    CHECK (`kind` IN ('STORY_FLAG', 'ITEM')),
  -- Exactly the column belonging to the kind is set, and no other. This is the constraint
  -- that the monolith's single untyped `target_id` could not express.
  CONSTRAINT `ck_choice_requirement_shape`
    CHECK ((`kind` = 'STORY_FLAG' AND `flag` IS NOT NULL AND `item_id` IS NULL)
        OR (`kind` = 'ITEM'       AND `item_id` IS NOT NULL AND `flag` IS NULL))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------------
-- choice_effect — what taking a choice does
--
-- Replaces `consequence` + `target_id` + `value_int`, and absorbs `choice_has_item`.
--
-- `travel` is not among the kinds: moving to the destination scene is already
-- `choice.destination_scene_id`, and the monolith's seed data shows what happens when the
-- same fact is stored twice — the rows disagree.
--
-- MODIFY_STAT enumerates the statistic. The monolith's data contains `lose_hp`, and there
-- is no hp anywhere in its schema; nothing caught it, because an untyped column cannot.
-- ---------------------------------------------------------------------------
CREATE TABLE `choice_effect` (
  `id`        INT         NOT NULL AUTO_INCREMENT,
  `choice_id` INT         NOT NULL,
  `ordinal`   INT         NOT NULL DEFAULT 0,
  `kind`      VARCHAR(16) NOT NULL,
  `item_id`   INT             NULL,
  `quest_id`  INT             NULL,
  `stat`      VARCHAR(16)     NULL,
  `amount`    INT             NULL,
  `flag`      VARCHAR(64)     NULL,
  PRIMARY KEY (`id`),
  KEY `ix_choice_effect_choice` (`choice_id`),
  KEY `ix_choice_effect_item` (`item_id`),
  KEY `ix_choice_effect_quest` (`quest_id`),
  CONSTRAINT `fk_choice_effect_choice`
    FOREIGN KEY (`choice_id`) REFERENCES `choice` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_choice_effect_item`
    FOREIGN KEY (`item_id`) REFERENCES `item` (`id`),
  CONSTRAINT `fk_choice_effect_quest`
    FOREIGN KEY (`quest_id`) REFERENCES `quest` (`id`),
  CONSTRAINT `ck_choice_effect_kind`
    CHECK (`kind` IN ('GRANT_ITEM', 'GRANT_QUEST', 'COMPLETE_QUEST', 'MODIFY_STAT', 'GRANT_FLAG')),
  CONSTRAINT `ck_choice_effect_stat`
    CHECK (`stat` IS NULL OR `stat` IN ('INTELLIGENCE', 'CHARISMA', 'FASHION')),
  CONSTRAINT `ck_choice_effect_shape`
    CHECK ((`kind` = 'GRANT_ITEM'
              AND `item_id` IS NOT NULL
              AND `quest_id` IS NULL AND `stat` IS NULL AND `amount` IS NULL AND `flag` IS NULL)
        OR (`kind` IN ('GRANT_QUEST', 'COMPLETE_QUEST')
              AND `quest_id` IS NOT NULL
              AND `item_id` IS NULL AND `stat` IS NULL AND `amount` IS NULL AND `flag` IS NULL)
        OR (`kind` = 'MODIFY_STAT'
              AND `stat` IS NOT NULL AND `amount` IS NOT NULL
              AND `item_id` IS NULL AND `quest_id` IS NULL AND `flag` IS NULL)
        OR (`kind` = 'GRANT_FLAG'
              AND `flag` IS NOT NULL
              AND `item_id` IS NULL AND `quest_id` IS NULL AND `stat` IS NULL AND `amount` IS NULL))
) ENGINE = InnoDB;
