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

**A consumer owns its queue and its binding** — *in the target state*. The publisher
would not know, and would not need to know, who is listening, so adding a second consumer
to an existing event would require no change on the publishing side. That is what keeps
the two sides independently deployable: if adding a consumer means editing the publisher,
they are one unit in two repositories.

> ### Interim: the publisher declares the queues
>
> **This is not what the code does today.** While the services are being built in
> parallel, **the publishing service declares the consumer queues and bindings for its own
> events**, as well as its exchange. See `beslutningslog.md` **ADR-021**, which amends
> ADR-004.
>
> **Why.** An exchange stores nothing. A topic exchange routes to the queues bound at the
> moment of publication and discards the message if there are none — and the publish call
> still succeeds. Until a consumer has started for the first time its queue does not
> exist, so every event published in that window is lost with no error on either side.
> That is the only genuine data-loss window in the design, and with seven services being
> written by different people, nobody can rely on deployment ordering to close it.
>
> **What it costs.** The publisher now has to know its consumer list. Adding a fourth
> consumer is a change in the publisher's repository, made by someone who does not own
> that consumer. Retiring one is too. The coupling is real; this section says so rather
> than presenting the interim arrangement as costless.
>
> **What a consumer still owns:** prefetch, concurrency, acknowledgement mode, and what it
> does with a dead-lettered message — everything in the two sections below except the act
> of declaring. Queue *arguments* are part of the declaration, so the dead-letter settings
> are fixed by the publisher: **a consumer that redeclares one of these queues with
> different arguments gets `PRECONDITION_FAILED` and will not start.** Bind to the queue
> that is already there; do not declare your own.
>
> **Where this goes.** Topology as code, applied at deploy time — a `definitions.json` or
> an init job in the pipeline. Each queue definition stays in its consumer's repository,
> so ownership is unchanged, but it is applied whether or not that consumer's pod has ever
> run. That is the right answer once there is a cluster to apply it to, and it is what
> returns us to ADR-004.

## Topology

One **topic exchange per publishing service**, named `<service>.events`:

```
storycontent.events   (topic, durable)
scenemanager.events   (topic, durable)
recap.events          (topic, durable)
account.events        (topic, durable)
```

Routing keys name the event in lowercase kebab-case: `scene-content.updated`,
`item-definition.updated`, `race-definition.updated`, `player-choice.made`.

Consumer queues are named `<consumer-service>.<purpose>` so that it is obvious from the
broker UI who owns what:

```
scenemanager.scene-content          binds storycontent.events / scene-content.updated
charactermanager.item-rules         binds storycontent.events / item-definition.updated
charactermanager.race-definitions   binds storycontent.events / race-definition.updated
storypathmanager.player-choice      binds scenemanager.events / player-choice.made
charactermanager.player-choice      binds scenemanager.events / player-choice.made
```

The first three already exist: StoryContentManager declares them on startup (ADR-021), so
they are a commitment to the consuming teams rather than a suggestion. Each has a
dead-letter queue beside it, `<queue-name>.dlq`, bound to `dlx`.

`charactermanager.race-definitions` was called `charactermanager.race-cache` in an earlier
draft. Renamed when the declaration moved to the publisher: a queue the publisher owns
should be named for what flows through it, not for what the consumer happens to do with
it. If CharacterManager later stops caching, the old name would be a lie in someone else's
broker.

Note the last two: one event, two queues, two independent consumers. In RabbitMQ a queue
is point-to-point, so publish-subscribe is achieved by binding several queues to the same
exchange and routing key — not by sharing one queue.

## How a consumer queue must be declared

**Who does this today:** the publishing service, per ADR-021 above. The requirements have
not changed — they are written here as the rule the declaration must satisfy, whoever
performs it — but if you are writing a consumer right now, your queue already exists and
your job is to bind a listener to it, not to declare it.

Three properties are not a matter of taste. A queue that gets them wrong looks correct
in development, where there is one consumer and a small backlog, and fails only under
the conditions the cluster creates.

| Property | Required value | What goes wrong otherwise |
|---|---|---|
| `durable` | `true` | The queue disappears on a broker restart, taking the backlog with it |
| `auto-delete` | `false` | The queue is deleted when the last consumer disconnects — which is exactly what an autoscaler does on scale-down, so the next published events are discarded until a consumer returns |
| `exclusive` | `false` | Only the declaring connection may consume, so a second instance cannot be started at all |

**One queue per consuming service, not one per instance.** Scaling a consumer means
running more instances against the *same* queue. They become competing consumers and
each message is delivered to exactly one of them, which is what distributes the work. If
every instance declared its own queue, each would be bound to the same routing key and
every instance would process every message.

**Set `prefetch` low.** Spring Boot's default is 250 unacknowledged messages per
consumer. With that default the first instance pulls the entire backlog into memory, and
further instances started by an autoscaler sit idle against a queue that looks empty —
the scaling has no effect, and the cause is invisible from outside:

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10
        concurrency: 3            # threads within one instance; cheaper than more pods
        default-requeue-rejected: false   # send failures to the DLX, not back to the queue
```

`default-requeue-rejected: false` is what makes the dead-letter configuration below
actually take effect. Left at its default, a message that cannot be processed is
requeued and redelivered forever.

**Scaling a consumer requires a commutative handler.** Competing consumers lose ordering:
two messages for the same entity can be handled concurrently. Which consumers may
therefore be scaled is decided per consumer — see `beslutningslog.md`, ADR-011.

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
