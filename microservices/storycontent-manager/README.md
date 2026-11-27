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

The pairs on the same row look alike on purpose. `quest_reward_item` is the reward an
author promised; `inventory_has_item` is what a player holds.

Two cases that get asked about:

- **Races are here**, not in CharacterManager, because `race.starting_chapter_id` is a
  mandatory reference into story content. On the state side it would be a foreign key into
  this database. `character_avatar.race_detail_id` over there is just an integer.
- **Quests are here** even though questing is state, because `quest.scene_id` is mandatory.
  The tracking of a quest per character is the state, not the quest.

## What it publishes

Three events, all on the topic exchange `storycontent.events`. The contract is
[`../contracts/storycontent-manager.asyncapi.yaml`](../contracts/storycontent-manager.asyncapi.yaml)
— **read that before writing a consumer**, not this file.

| Routing key | Carries | Consumer |
|---|---|---|
| `scene-content.updated` | the whole scene aggregate: the scene, its choices with their requirements and effects, its NPC ids and quest ids | SceneManager |
| `item-definition.updated` | what a consumer needs to enforce rules: type, slot, requirements, modifiers | CharacterManager |
| `race-definition.updated` | a race, its starting chapter **and that chapter's starting scene** | CharacterManager |

**This service declares the consumer queues as well as the exchange.** That is interim, and
a departure from the rule that a consumer owns its own queue. An exchange stores nothing:
until a consumer has started for the first time its queue does not exist, and every event
published in that window is discarded with the publish call still succeeding.

| Event | Queue | Dead-letter queue |
|---|---|---|
| `scene-content.updated` | `scenemanager.scene-content` | `scenemanager.scene-content.dlq` |
| `item-definition.updated` | `charactermanager.item-rules` | `charactermanager.item-rules.dlq` |
| `race-definition.updated` | `charactermanager.race-definitions` | `charactermanager.race-definitions.dlq` |

The `dlx` exchange and the three dead-letter queues are declared here too. RabbitMQ does
not check `x-dead-letter-exchange` at declare time, and an exchange with nothing bound to
it discards what it receives, so without them rejected messages went down a dead-letter
path that only looked configured.

The cost: this service now knows its consumer list. A fourth consumer means a change
*here*, made by someone who does not own that consumer.

Consumers still own prefetch, concurrency, acknowledgement and what they do with a
dead-lettered message — see `../contracts/README.md`. They do **not** own the queue
arguments: redeclaring one of these queues differently fails with `PRECONDITION_FAILED`.

### The version field

Every published aggregate carries a version: `sceneVersion`, `itemVersion`, `raceVersion`.
It increments on **every change a consumer can observe**, and a consumer must discard any
message whose version is not newer than what it already holds.

- Publishing **without** incrementing produces a message that looks older than what the
  consumer has, so the consumer throws it away. Nothing is logged anywhere.
- A change a consumer **cannot** observe must not increment. Renaming an NPC is the
  example: only `npcIds` travels in the scene payload.

One consequence is easy to forget and has its own test: changing a chapter's starting scene
republishes **every race that starts in that chapter**, because the race payload carries
that scene id.

### Events are published after the transaction commits

A service method records *that* something changed; `ContentEventDispatcher` sends it on an
`AFTER_COMMIT` listener. Publishing inside the transaction would put a message on the
broker that survives a rollback, and a consumer holding a version this service never kept
would then discard every later event for that entity as stale.

The remaining gap: if the process dies between the commit and the publish, the change is
saved and nobody is told. The fix is a transactional outbox, which is not implemented.

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
failure:

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
alter the schema, so starting the service is itself a check that the entities and the SQL
agree.

`db-init/02_seed_demo_content.sql` is development seed data. It runs only when the
container initialises an empty data directory, so it never overwrites authored content.
The integration tests deliberately do **not** load it.

## The API

Admin only — every route writes content, and content is written by the writing team.

**Send `X-API-VERSION: 1` on every request.** The version is a header, not a path segment,
matching RecapManager. A versioned endpoint rejects a request without it; unversioned
routes — Swagger, the actuator health endpoints — do not need it.

```powershell
curl.exe -s "http://localhost:8081/admin/scenes" -H "X-API-VERSION: 1"
```

| | |
|---|---|
| `/admin/chapters` | CRUD; `PUT /{id}/starting-scene` is separate on purpose — it republishes races |
| `/admin/scenes` | CRUD; `DELETE` retires rather than deletes |
| `/admin/scenes/{id}/choices` | choices are a sub-resource, because they are part of the scene aggregate |
| `/admin/scenes/{id}/npcs` | replaces the set of NPCs placed in a scene |
| `/admin/scenes/{sceneId}/quests`, `/admin/quests/{id}` | |
| `/admin/items` | `DELETE` retires |
| `/admin/npcs` | `DELETE` is a real delete; nothing a player holds refers to an NPC |
| `/admin/races` | `DELETE` retires |

Two things that look like omissions and are not:

- **`DELETE` usually retires.** Recorded player paths reference scenes and items, and no
  foreign key stops an author removing one now. The destructive version is not offered
  rather than guarded.
- **There is no read endpoint for other services.** CharacterManager caches
  `race-definition.updated` instead of asking for a race per character creation. A lookup
  here would make the cache optional and bring the dependency back.

> **Authorization is not wired up — these routes are open to anyone who can reach the
> port.** It waits for AccountManager, so the token handling can be built against tokens
> someone can actually issue rather than fixtures.
>
> **Do not expose this service outside a laptop or a local cluster until it is done.**
>
> There is a shape to copy when the time comes: RecapManager validates an RS256 token
> against a public key and expects a single `role` claim of exactly `ROLE_USER` or
> `ROLE_ADMIN`. Two things to get right — the actuator probes must stay open, since
> Kubernetes sends no token, and `ApiVersioningTest` will need `spring-security-test`.

## Tests

```powershell
.\mvnw test      # unit tests only, no Docker needed
.\mvnw verify    # adds the integration tests, which DO need Docker
```

`*Test.java` runs under surefire, `*IT.java` under failsafe.

| | |
|---|---|
| `ContractShapeTest` | compares the published AsyncAPI document field by field against what this service serialises, in both directions |
| `PayloadAssemblerTest` | the entity-to-payload mapping |
| `ContentPublishingIT` | real MySQL built from `db-init/01_schema.sql`, real RabbitMQ, a queue bound the way a consumer would bind one. Tests the version discipline |

`ContentPublishingIT` builds the database from the committed SQL rather than from the
entities, so `ddl-auto: validate` makes starting the context a check that the two agree.

**If `verify` fails before any test runs, check Docker Desktop is actually running.**
Testcontainers does not say so clearly.
