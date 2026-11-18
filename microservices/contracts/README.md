# Messaging contracts

Conventions that apply to every service in the system, and the place where each
service's published events are documented.

Broker: **RabbitMQ**. Contracts are written as **AsyncAPI 3.0**, which is also the
documentation format the project is required to deliver — so the contract and the
documentation are the same artifact rather than two things that drift apart.

| File | Publisher |
|---|---|
| `storycontent-manager.asyncapi.yaml` | StoryContentManager |
| *(to be added)* | SceneManager, RecapManager, AccountManager |

---

## Ownership

**A publisher owns its exchange and the schema of its events.** Nobody else declares
them and nobody else changes them.

**A consumer owns its queue and its binding.** The publisher does not know, and must not
need to know, who is listening. Adding a second consumer to an existing event requires
no change on the publishing side.

This is what keeps the two sides independently deployable. If adding a consumer meant
editing the publisher, they would be one unit in two repositories.

## Topology

One **topic exchange per publishing service**, named `<service>.events`:

```
storycontent.events   (topic, durable)
scenemanager.events   (topic, durable)
recap.events          (topic, durable)
account.events        (topic, durable)
```

Routing keys name the event in lowercase kebab-case: `scene-content.updated`,
`item-definition.updated`, `player-choice.made`.

Consumer queues are named `<consumer-service>.<purpose>` so that it is obvious from the
broker UI who owns what:

```
scenemanager.scene-content        binds storycontent.events / scene-content.updated
charactermanager.item-rules       binds storycontent.events / item-definition.updated
storypathmanager.player-choice    binds scenemanager.events / player-choice.made
charactermanager.player-choice    binds scenemanager.events / player-choice.made
```

Note the last two: one event, two queues, two independent consumers. In RabbitMQ a queue
is point-to-point, so publish-subscribe is achieved by binding several queues to the same
exchange and routing key — not by sharing one queue.

## Dead-lettering

Every consumer queue declares a dead-letter exchange:

```
x-dead-letter-exchange: dlx
x-dead-letter-routing-key: <queue-name>.dlq
```

A message that cannot be processed must end up somewhere inspectable rather than being
lost or redelivered forever. Retry counts and backoff are not yet decided (see
`../OPEN-DESIGN-ISSUES.md`, 3.2), but the dead-letter destination can be declared from
the start.

## Message envelope

Every message has the same outer shape. Only `payload` differs per event.

```json
{
  "eventId": "0f9c2d1e-7a41-4f0c-9d8e-3b2a1c5e6f70",
  "eventType": "scene-content.updated",
  "occurredAt": "2026-11-17T13:42:01.284Z",
  "schemaVersion": 1,
  "payload": { }
}
```

| Field | Purpose |
|---|---|
| `eventId` | Consumer-side deduplication. Delivery is at-least-once, so a consumer that is not idempotent will eventually corrupt its own state |
| `eventType` | Same value as the routing key. Lets a consumer bound to a wildcard tell messages apart without inspecting the payload |
| `occurredAt` | When the fact happened, not when it was published. Used for diagnostics and correlation |
| `schemaVersion` | Incremented on a breaking change to `payload`. Additive changes do not increment it |

The same values are also set as AMQP properties, so the broker UI and tooling are useful
without decoding the body: `message_id` = `eventId`, `type` = `eventType`,
`content_type` = `application/json`, `timestamp` = `occurredAt`, and `delivery_mode` = 2
so messages survive a broker restart.

## Schema evolution

Consumers must ignore fields they do not recognise. That makes adding a field a
non-breaking change that can be deployed without coordination.

Removing a field, renaming one, or changing its meaning is breaking: publish both
versions side by side on different routing keys until every consumer has moved, then
retire the old one. `schemaVersion` exists so a consumer can refuse a message it was not
written for instead of silently misreading it.

## What consumers must assume

- **Delivery is at-least-once.** Deduplicate on `eventId`
- **Ordering is only guaranteed within a queue.** Where order matters, it must be stated in the contract and enforced by a single consumer instance for that key
- **Events are facts about the past.** A consumer reacts to them; it never rejects one as invalid. Validation belongs on the publishing side, before the event exists
