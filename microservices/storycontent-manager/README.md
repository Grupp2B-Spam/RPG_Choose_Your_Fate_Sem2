# StoryContentManager

Owns the **authored content** of Choose Your Fate — everything the writing team produces
before anyone plays — and publishes it so the rest of the system can react.

It is the head of the content pipeline: it consumes nothing and calls nobody.

## What it owns, and what it does not

The dividing line is **content versus state**: authored material that exists before anyone
plays belongs here; anything a player produced belongs elsewhere.

| This service | Somewhere else |
|---|---|
| `chapter`, `scene`, `choice` | `character_avatar`, `character_details` |
| `quest`, `quest_reward_item` | `character_has_quest` |
| `item`, `item_requirement`, `item_modifier` | `inventory`, `inventory_has_item`, `equipment` |
| `npc`, `scene_npc` | `character_path`, `character_path_choice` |
| `race` | `account` |

Watch the pairs on the same row — they look alike on purpose. `quest_reward_item` is the
reward an author *promised*; `inventory_has_item` is what a player actually *holds*. Same
shape, opposite sides of the boundary. What tells them apart is who wrote the row.

Two cases that are asked about often:

- **Races are here**, not in CharacterManager, because `race.starting_chapter_id` is a
  mandatory reference into story content. Putting races on the state side would give
  CharacterManager a foreign key into this database, which is the coupling the
  decomposition exists to remove. `character_avatar.race_detail_id` becomes a plain
  integer over there, which is harmless.
- **Quests are here** even though questing is state, because `quest.scene_id` is mandatory
  — a quest cannot exist without the scene that offers it. The *tracking* of a quest per
  character is the state, not the quest.

The full reasoning, and a register of every case that was genuinely ambiguous, is in
`beslutningslog.md` (ADR-015 and ADR-015a), which is kept outside the repository.

## What it publishes

Three events, all on the topic exchange `storycontent.events`. The contract is
[`../contracts/storycontent-manager.asyncapi.yaml`](../contracts/storycontent-manager.asyncapi.yaml)
— **read that before writing a consumer**, not this file.

| Routing key | Carries | Consumer |
|---|---|---|
| `scene-content.updated` | the whole scene aggregate: the scene, its choices with their requirements and effects, its NPC ids and quest ids | SceneManager |
| `item-definition.updated` | what a consumer needs to enforce rules: type, slot, requirements, modifiers | CharacterManager |
| `race-definition.updated` | a race, its starting chapter **and that chapter's starting scene** | CharacterManager |

**This service declares the consumer queues as well as the exchange** — an interim
arrangement that amends ADR-004; see ADR-021.

The rule used to be that a consumer declares its own queue, which is what keeps publisher
and consumer independently deployable. It was suspended because an exchange stores
nothing: until a consumer has started for the first time its queue does not exist, and
every event published in that window is discarded with the publish call still succeeding.
With seven services being built in parallel, nobody can rely on deployment ordering to
close that window.

| Event | Queue | Dead-letter queue |
|---|---|---|
| `scene-content.updated` | `scenemanager.scene-content` | `scenemanager.scene-content.dlq` |
| `item-definition.updated` | `charactermanager.item-rules` | `charactermanager.item-rules.dlq` |
| `race-definition.updated` | `charactermanager.race-definitions` | `charactermanager.race-definitions.dlq` |

The `dlx` exchange and the three dead-letter queues are declared here too. The convention
document had required `x-dead-letter-exchange: dlx` on every consumer queue and nothing
had ever declared `dlx` — and RabbitMQ does not check that argument at declare time, so
rejected messages were being dropped by a dead-letter path that looked configured.

**The cost, stated plainly:** this service now knows its consumer list. A fourth consumer
means a change *here*, made by someone who does not own that consumer. That is what
ADR-021 traded away, and it is why the arrangement is interim rather than the target.

Consumers still own prefetch, concurrency, acknowledgement and what they do with a
dead-lettered message — see `../contracts/README.md`. They do **not** own the queue
arguments: redeclaring one of these queues differently fails with `PRECONDITION_FAILED`.

### The version field is the thing to understand

Every published aggregate carries a version: `sceneVersion`, `itemVersion`,
`raceVersion`. It increments on **every change a consumer can observe**, and a consumer
must discard any message whose version is not newer than what it already holds.

Both halves of that sentence are load-bearing:

- A change that is published **without** incrementing produces a message that looks older
  than what the consumer has, so the consumer throws it away. Nothing is logged anywhere.
- A change a consumer **cannot** observe must not increment. Renaming an NPC is the
  example: only `npcIds` travels in the scene payload, so a rename changes nothing on the
  wire.

One consequence is easy to forget and has its own test: changing a chapter's starting
scene republishes **every race that starts in that chapter**, because the race payload
denormalises that scene id. Nothing about the races was edited; the correct answer for
them changed anyway.

### Events are published after the transaction commits

A service method records *that* something changed; `ContentEventDispatcher` sends it on an
`AFTER_COMMIT` listener. Publishing inside the transaction would put a message on the
broker that survives a rollback — and because of the version rule above, a consumer
holding a version this service never kept would then discard every later event for that
entity as stale. One rollback would freeze that scene in the read model permanently.

The remaining gap: if the process dies between the commit and the publish, the change is
saved and nobody is told. The correct fix is a transactional outbox. It is not
implemented — see `OPEN-DESIGN-ISSUES.md` 4.5.

## Running it

### With docker compose

From the repository root, which also brings up RabbitMQ:

```powershell
docker compose --profile prod up -d
```

Or this service and its database alone, from this directory:

```powershell
docker compose up -d
```

| | |
|---|---|
| HTTP | 8081 |
| MySQL | `story-content-vault`, host port **3309** (3307 and 3308 are the monolith's) |
| Database | `story_content` |
| OpenAPI UI | http://localhost:8081/swagger-ui.html |

### From the IDE

The defaults in `application.yml` point at the compose database on 3309 and RabbitMQ on
localhost, so starting the application class works with no configuration — **as long as
the containers are up**.

**Check nothing is already on 8081 before you start it.** Running it twice is a confusing
failure, and killing someone else's instance is worse:

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
```

### Configuration

Every value comes from the environment with a local default (convention 1), so the same
image runs unchanged in compose and in the cluster:

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8081` |
| `MYSQL_URI` | `jdbc:mysql://localhost:3309/story_content` |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / `123456` |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `guest` / `guest` |

Health: `/actuator/health/liveness` and `/actuator/health/readiness`. Readiness includes
the broker **and** the database, because every write here both persists and publishes.
Liveness includes neither — losing a dependency is not a reason to restart.

## The schema is owned here, as SQL

`db-init/01_schema.sql` is the artifact the database is created from. It is **not** a copy
of the monolith's schema; its header explains each deliberate difference.

`spring.jpa.hibernate.ddl-auto` is `validate`, never `update`. Hibernate is not allowed to
alter the schema, because then the running shape and the committed shape could differ and
the committed one is what everyone else reads. Starting the service is therefore itself a
check that the entities and the SQL agree.

`db-init/02_seed_demo_content.sql` is development seed data. It runs only when the
container initialises an empty data directory, so it never overwrites authored content.
The integration tests deliberately do **not** load it.

## The API

Admin only — every route writes content, and content is written by the writing team.
Versioned at `/v1` from the start.

| | |
|---|---|
| `/admin/v1/chapters` | CRUD; `PUT /{id}/starting-scene` is separate on purpose — it republishes races |
| `/admin/v1/scenes` | CRUD; `DELETE` retires rather than deletes |
| `/admin/v1/scenes/{id}/choices` | choices are a sub-resource, because they are part of the scene aggregate |
| `/admin/v1/scenes/{id}/npcs` | replaces the set of NPCs placed in a scene |
| `/admin/v1/scenes/{sceneId}/quests`, `/admin/v1/quests/{id}` | |
| `/admin/v1/items` | `DELETE` retires |
| `/admin/v1/npcs` | `DELETE` is a real delete; nothing a player holds refers to an NPC |
| `/admin/v1/races` | `DELETE` retires |

Two things that look like omissions and are not:

- **`DELETE` usually retires.** Recorded player paths reference scenes and items, and
  after the decomposition no foreign key stops an author from removing one. The
  destructive version is not offered rather than guarded.
- **There is no read endpoint for other services.** CharacterManager caches
  `race-definition.updated` instead of asking for a race per character creation. A
  convenient lookup here would make the cache optional in practice and the dependency
  would quietly come back (ADR-017).

> **Authorization is not wired up.** These routes are currently open. The role claim they
> need is AccountManager's to define (`OPEN-DESIGN-ISSUES.md` 1.3).

## Tests

```powershell
.\mvnw test      # unit tests only, no Docker needed
.\mvnw verify    # adds the integration tests, which DO need Docker
```

`*Test.java` runs under surefire, `*IT.java` under failsafe.

| | |
|---|---|
| `ContractShapeTest` | parses the published AsyncAPI document and compares it field by field against what this service serialises, in both directions. It fails if a field is added to the code and not the contract, or the other way round |
| `PayloadAssemblerTest` | the entity-to-payload mapping |
| `ContentPublishingIT` | real MySQL built from `db-init/01_schema.sql`, real RabbitMQ, a queue bound the way a consumer would bind one. Tests the version discipline, which is what consumer correctness rests on |

`ContentPublishingIT` builds the database from the committed SQL rather than from the
entities, so `ddl-auto: validate` makes starting the context a check that the two agree.

**If `verify` fails before any test runs, check Docker Desktop is actually running.**
Testcontainers cannot tell you that in a useful way.
