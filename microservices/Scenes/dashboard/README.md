# Scene Control - standalone local frontend

Scene Control is a zero-dependency browser UI with a tiny Node.js HTTP server. It lives beside `scene-manager/` and `scene-indexer/`, but runs independently. No changes are required in either Java service.

## Run separately on your laptop

**Requires Node.js 20 or newer.** With the existing SceneManager / SceneIndexer stack running in Docker, open a *different* terminal:

```bash
cd scenes/dashboard
npm start
```

Open <http://127.0.0.1:3000>.

The dashboard itself also opens if MongoDB, RabbitMQ or the Java services are stopped. It shows offline status, and the endpoint reference and event data structures remain available.

### Optional: launch the frontend as a separate Docker service

The parent `compose.yaml` defines the optional `dashboard` profile. From `scenes/`:

```bash
# Existing backend services, without the dashboard

docker compose up --build -d

# Launch only the dashboard service; backend services already running

docker compose --profile dashboard up --build -d dashboard
```

Open <http://127.0.0.1:3000>. Stop only the dashboard with `docker compose stop dashboard`. The dashboard container uses Compose DNS names (`scene-manager:8080` and `rabbitmq:15672`) while the locally started Node process uses host loopback addresses.

## What's in the UI

- Service status for SceneManager and RabbitMQ.
- Docker and host addresses for the broker (AMQP 5672), management UI (HTTP 15672) and SceneManager (HTTP 8080).
- Live queue depths, consumer counts and routing keys for `scene-indexer.scene-changed`, `scene-indexer.dead` and both demo `choice.made` queues.
- API explorer for all three specified SceneManager REST endpoints; inspect JSON replies and rendered choices.
- Mock StoryContentManager publisher: edit a `scene.changed` event (UPSERT/DELETE), or publish three demo scene events via a single button.
- Data contract reference for `SceneDocument`, `Choice`, `SceneChangedEvent`, and `ChoiceMadeEvent`.

The mock is a **publisher**, not a second queue: it sends events to the RabbitMQ Management API via the dashboard's own Node server. RabbitMQ routes the event from exchange `game.events` with key `scene.changed` to queue `scene-indexer.scene-changed`. The actual RabbitMQ container's IP is assigned dynamically; `rabbitmq:5672` is the stable name **inside** the Compose network.

The dashboard intentionally does not consume `choice.made` events; it only shows subscriber queue depths. Consuming a queue to "peek" can remove messages and affect testing.

## Backend configuration

The dashboard's HTTP proxy keeps RabbitMQ credentials **server-side** and avoids browser CORS problems. Override defaults with environment variables:

| Variable | Default (host process) | Notes |
|---|---|---|
| `PORT` | `3000` | Dashboard HTTP port |
| `DASHBOARD_HOST` | `127.0.0.1` | Binds to loopback by default |
| `SCENE_MANAGER_URL` | `http://127.0.0.1:8080` | Java SceneManager origin |
| `RABBIT_HTTP_URL` | `http://127.0.0.1:15672` | RabbitMQ Management API origin used by the server |
| `SCENE_MANAGER_PUBLIC_URL` | `http://127.0.0.1:8080` | Host/browser-facing address displayed in the UI |
| `RABBIT_PUBLIC_URL` | `http://127.0.0.1:15672` | Host/browser-facing RabbitMQ management UI link |
| `RABBIT_USER` | `dev` | Development-only broker user |
| `RABBIT_PASS` | `dev-local-only` | Development-only broker password |
| `AMQP_HOST_ADDRESS` | `127.0.0.1:5672` | Host-facing AMQP address shown in the UI |

These credentials match the existing demonstration Compose stack. **Never expose this dashboard publicly**: it intentionally allows a local developer to publish arbitrary scene updates and submit choices, and it does not implement authentication or authorization. Do not use the default credentials outside local development.

## Demo workflow

1. Start the existing `scenes` backend stack with `docker compose up --build -d`.
2. Start the dashboard independently with `npm start` in `scenes/dashboard`.
3. Press **Seed 3 example scenes** (requires the broker and SceneIndexer).
4. Under **API explorer**, run `GET /scenes/demo-start` and `GET /scenes/demo-start/nextscenes`.
5. Run `POST /scenes/demo-start/choices/left` with `demo-character` and observe the `choice.made` subscriber queues.

The example IDs are prefixed `demo-` to avoid overwriting the project's original sample scenes. Version `1` seeding is idempotent and cannot replace a higher version in MongoDB. Scene indexing is asynchronous; refresh or retry GET after publishing.

## Test the dashboard

Run without the Java services or Docker:

```bash
cd scenes/dashboard
npm test
```

Uses the built-in `node:test` runner with fake upstream HTTP services. There are no npm dependencies, package installation steps or lockfile.

## Important architecture boundaries

- SceneIndexer: sole MongoDB writer; consumes versioned `scene.changed` events.
- SceneManager: read-only MongoDB service; validates choice membership and publishes `choice.made` events.
- The dashboard is local developer tooling and **not** an API gateway or part of the game's production architecture.
- Character position/progress, authentication, idempotent choice requests and downstream service consumption remain intentionally unimplemented in the existing project.
