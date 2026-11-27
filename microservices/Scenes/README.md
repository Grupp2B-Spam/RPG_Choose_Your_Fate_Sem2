# Choose Your Fate — minimal SceneIndexer and SceneManager

Two independent **Java 21 / Spring Boot 4.1.2** services, **MongoDB 7** and **RabbitMQ 4**.
This is a teaching/demo vertical slice based on the uploaded architecture description, not a production-ready game backend.

## Architecture and ownership

```text
StoryContentManager (not implemented; represented by seed_scenes.py)
    │ scene.changed {UPSERT|DELETE, sceneId, version, full scene}
    ▼
RabbitMQ: game.events (topic exchange)
    │ scene.changed → scene-indexer.scene-changed queue
    ▼
SceneIndexer ────────── writes MongoDB.scenes (indexer readWrite user)
                                       │
                                       │ read only (manager DB user has read role)
                                       ▼
Player → GET/POST /scenes/* → SceneManager
                                       │
                                       │ choice.made {snapshot + effects}
                                       ▼
                               RabbitMQ: game.events
                                   /          \
             demo.storypath.choice-made     demo.character.choice-made
                  (stub queue)                    (stub queue)
```

**CQRS division:** SceneIndexer is the only service writing the Mongo scene projection. SceneManager
never edits scene records and has a **MongoDB read-only credential**. Both run independently.

**Document alignment:** SceneIndexer consumes updates from StoryContentManager, owns MongoDB
scenes; SceneManager exposes `GET /scenes/{id}`, `GET /scenes/{id}/nextscenes`,
`POST /scenes/{id}/choices/{choiceId}`, and produces a message for StoryPathManager and
CharacterManager. Source: supplied *Choose Your Fate* report, section 2.2, pages 6–8.

## Run

Requires Docker with Compose and Python 3 (for the local event publisher). Start:

```bash
docker compose up --build -d
python3 scripts/seed_scenes.py
```

Query the scenes (allow a moment for asynchronous indexing):

```bash
curl -i http://localhost:8080/scenes/start
curl -i http://localhost:8080/scenes/start/nextscenes
```

Submit a choice:

```bash
curl -i -X POST http://localhost:8080/scenes/start/choices/left \
  -H 'Content-Type: application/json' \
  -d '{"characterId":"char-123"}'
python3 scripts/peek_events.py
```

The REST response is **202 Accepted**, and the same `choice.made` message appears in
both demo subscriber queues. Inspect queues, routing and dead-lettered messages in the
RabbitMQ UI at <http://localhost:15672> (`dev` / `dev-local-only`).

Run unit tests locally with Maven 3.9+ and Java 21:

```bash
(cd scene-indexer && mvn test)
(cd scene-manager && mvn test)
```

Reset demo data (destructive): `docker compose down -v`.

## HTTP shape

- `GET /scenes/start` → scene with `id`, `version`, `title`, `text`, `choices`.
- `GET /scenes/start/nextscenes` → `[{"id":"mill","title":"Old Mill"}, ...]`.
- `POST /scenes/start/choices/left` with `{"characterId":"char-123"}` →
  `202 Accepted`, `{"eventId":"...","nextSceneId":"mill"}`.
- Missing/deleted scene → **404**. Choice not found in that scene → **400**.

## Event contracts (examples)

**StoryContentManager → SceneIndexer** through exchange `game.events`, key `scene.changed`:

```json
{
  "sceneId": "start",
  "version": 1,
  "operation": "UPSERT",
  "title": "Crossroads",
  "text": "A path splits around an old tree.",
  "choices": [{"id":"left","text":"Go toward the mill","nextSceneId":"mill","effects":{"courage":1}}]
}
```

To delete a scene publish `{"sceneId":"start","version":2,"operation":"DELETE"}`.
Indexer retains a tombstone (`deleted: true`) rather than physically deleting the document,
so a late version-1 upsert cannot resurrect it. SceneManager hides tombstones.

**SceneManager → subscribers** through exchange `game.events`, key `choice.made`:

```json
{
  "eventId": "generated-uuid",
  "characterId": "char-123",
  "sceneId": "start",
  "sceneVersion": 1,
  "sceneTitle": "Crossroads",
  "sceneText": "A path splits around an old tree.",
  "choiceId": "left",
  "choiceText": "Go toward the mill",
  "nextSceneId": "mill",
  "effects": {"courage": 1},
  "occurredAt": "2026-11-11T12:00:00Z"
}
```

The snapshot lets StoryPathManager store the original scene/choice text even after a scene
is edited. CharacterManager can use `effects`, subject to its own validation and transaction.

## Boundaries, assumptions and deliberately missing features

1. **Broker choice is an assumption.** The report says "Eventqueue" but does not specify
   RabbitMQ, topic names, exchange names, message schema or event versioning. Those are
   pragmatic additions in this sample, not requirements copied from the report.
2. **Choice checks are only local.** A choice must exist in the requested scene. The report
   explicitly leaves checking the player's current scene open pending coordination with
   StoryPathManager. This demo does **not** verify position, prerequisites, inventory, or stats.
3. **Security is absent.** The HTTP body accepts any `characterId`; production must authenticate
   the player and authorize access to that character, ideally using AccountManager identity.
4. **Downstream services are not implemented.** The two `demo.*` queues are stand-ins to
   visualize RabbitMQ fan-out. In production, StoryPathManager and CharacterManager should
   declare and consume their own durable queues and process `eventId` idempotently.
5. **Request retries are not yet idempotent.** A repeat HTTP POST creates a new `eventId`
   and may apply effects twice. Add a client request ID, durable deduplication and/or a
   transactional outbox before relying on this for real gameplay.
6. **Publisher confirmations/outbox/retries and API gateway/JWT integration are missing.**
   A broker failure may produce an error, and successful `send` alone does not prove a
   downstream service processed the event. An indexer poison message is dead-lettered to
   `scene-indexer.dead` instead of retried indefinitely.
7. **Scene versioning is required from StoryContentManager.** The indexer uses a Mongo atomic
   version predicate and tombstones to protect against duplicate/out-of-order delivery.
   Events without a positive monotonic version are sent to the dead-letter queue.
8. **The `effects` JSON shape is only a placeholder contract.** Coordinate its final schema
   with CharacterManager. Likewise agree whether /nextscenes should reveal spoilers.
9. **Local credentials are for demonstrations only.** Replace them with secrets and remove
   exposed ports before deploying anywhere outside your development machine.

For the complete assignment, add an integration test with actual MongoDB and RabbitMQ
(e.g., Testcontainers), plus tests for stale upserts, deletes, unavailable broker,
authorization, and repeated POSTs.
