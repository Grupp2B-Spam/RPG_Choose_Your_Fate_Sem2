# TextToSpeechManager serverless operation

The accepted design scales the whole TextToSpeechManager application to zero with
Knative, keeps audio in MinIO and coordinates generation in a separate MySQL
metadata vault. This follows the Patrick diagram's MinIO audio store while making
metadata ownership explicit. The service does not retrieve narratives from the
monolith or share RecapManager's database.

This guide explains the implementation and its cluster prerequisites. Accepted
decisions and their reasons are recorded separately in
[TEXT_TO_SPEECH_MANAGER_DECISIONS.md](TEXT_TO_SPEECH_MANAGER_DECISIONS.md).
No Kubernetes manifests are supplied.

## Why this can scale to zero

A replica is a running copy of the application's container. At zero replicas,
there is no application process to receive gRPC calls or consume RabbitMQ. Knative
supplies an activation path that holds an arriving request while a replica starts.
NGINX ingress alone cannot do this. RabbitMQ, MinIO, MySQL and the activation and
event-delivery infrastructure remain available while application replicas stop.
The first request can incur scheduling, image retrieval and Java startup delays.
See [Knative request flow](https://knative.dev/docs/serving/request-flow/).

Serverless does not prohibit gRPC. The container must support the platform's
HTTP/2 routing and request lifecycle. Armeria serves native gRPC, event-delivery
HTTP and probes on one listener, while Spring Boot supplies configuration and
dependency injection. The application honors `PORT` and supports cleartext HTTP/2
behind a trusted TLS-terminating platform proxy. It has no permanent queue
subscription or scheduled worker in production. See
[Knative's runtime contract](https://github.com/knative/specs/blob/main/specs/serving/runtime-contract.md)
and [Armeria gRPC services](https://armeria.dev/docs/server/grpc/).

Generation runs inside an event request. Returning success means the audio and its
metadata are ready; the service never responds successfully and leaves generation
running on an untracked background thread. The alternative of keeping a small
gRPC API running and scaling only queue workers to zero was considered; the user
selected whole-application scale-to-zero despite the additional activation needs.

## Why audio and metadata use different stores

MinIO stores `speech/v1/<contentHash>.mp3` as a private audio object. MySQL stores
the exact synthesis inputs, status, attempts, lease token and timestamps in the
service-owned `speech_job` table. No audio bytes are stored in MySQL, and no audio
is persisted on a container filesystem.

| Metadata option | Benefit | Drawback |
| --- | --- | --- |
| MinIO objects only | Avoids another database | Coordination needs conditional object writes and lease recovery; job inspection and querying are less convenient |
| Separate MySQL vault, selected | Uniqueness and atomic conditional updates simplify coordination across replicas | Adds credentials, backups and connection capacity; it remains available at zero application replicas |

MySQL cannot transact atomically with MinIO or ElevenLabs. The implementation
therefore uses a deterministic object key and recovery checks. An uploaded object
is reused if an instance stopped before committing `READY`. This avoids ordinary
duplicate generation but cannot promise exactly one billed provider call after
every possible crash.

## Queue generation and lookup flow

1. RecapManager commits the narrative and its existing publication outbox. It
   publishes the current flat `RecapGenerated` JSON to the durable topic exchange
   `choose-your-fate.recap`, routing key `recap.generated.v2`. Version 2 contains
   the saved text and recap metadata, without audio settings or an audio hash.
2. The queue-delivery infrastructure wraps that unchanged body in a CloudEvent and
   sends it to `POST /events/recap` through the Knative route. A delivery-component
   JWT authenticates the request; no player's token is placed in the queue.
3. TextToSpeechManager validates the text, selects its configured voice/model/format,
   calculates the audio hash, inserts the job
   if absent and atomically claims its lease. Another replica cannot acquire the
   same unexpired lease.
4. The worker checks MinIO first, generates missing audio through ElevenLabs,
   uploads it and commits `READY` only if it still holds the lease token.
5. It returns HTTP 204 only after completion. Invalid input returns 400; missing or
   unauthorized delivery identity returns 401/403. Busy work, generation failures
   and unavailable dependencies return 503, requiring retry or dead-lettering.
6. When requests finish and the idle window passes, Knative may stop every
   application replica. Audio, metadata and queued work remain durable.

The gRPC wire contract stays
`choose_your_fate.speech.v1.SpeechService/GetOrCreateSpeech`. The Java package is
local to each service. The request now contains only `text = 2` and `wait_millis = 6`;
removed request fields 1, 3, 4 and 5 and their names are reserved against reuse.
The response still returns the TTS-calculated hash, status and audio URL. Update
both services together: older RecapManager clients calculated an expected audio
hash themselves, which is incompatible with service-owned synthesis configuration.
`READY` returns a newly signed audio URL without provider work. On a `QUEUED` miss,
the API publishes a persistent `SpeechRequested` event to the same consumer-owned
queue and requires a routed broker confirmation before returning `PENDING`. This
version 2 event references only the stored job hash, not synthesis settings. The
worker loads the job's original settings from MySQL, so a configuration update
does not change work already queued. The API polls job status within the requested
wait. A publication failure becomes
gRPC `UNAVAILABLE`; a later request can repair the interrupted handoff. Confirmed
publication proves broker acceptance, not completed generation.

Inside TextToSpeechManager, both paths use the SHA-256 identity over four-byte
length-prefixed UTF-8
values: `recap-speech-v1`, exact text, voice, model and output format. The configured
voice/model/MP3 format is enforced before paid generation. Repeated deliveries and
queue/RPC races therefore converge on the same job and audio identity.

The general contracts README describes a different event envelope. This consumer
preserves RecapManager's actual flat event contract; changing it requires a
coordinated contract version, not silently introducing `payload` here.

## Required cluster integration

The cluster owner must provide these components and settings; this change does not
install or create them:

- Knative Serving with a working networking layer and zero minimum application
  replicas. Configure its application port for native HTTP/2/gRPC (`h2c`). The
  default local port is 9093; Knative's injected `PORT` takes precedence.
- Queue-to-HTTP delivery with authenticated CloudEvents, bounded backoff/retries
  and dead-letter handling. The platform component must retain responsibility for
  failed deliveries until success or a durable dead-letter handoff. A direct
  RabbitMQ consumer inside a scaled-to-zero application cannot activate itself.
- A durable `text-to-speech-manager.recap-generated` queue bound to the existing
  publisher-owned exchange/routing key above. Align `TTS_QUEUE` and the actual
  delivery topology. Production application startup does not declare or consume
  queues; the service's delivery infrastructure provisions them.
- A service-owned MySQL database and private MinIO bucket, independently durable
  and backed up. Startup creates the metadata table inside an existing database;
  it does not provision a database, bucket or root credentials.
- Network access to those dependencies, RabbitMQ, trusted key endpoints and
  ElevenLabs. Restrict the event route to delivery infrastructure. Keep application
  credentials in the deployment secret mechanism.

RabbitMQSource is a possible queue-to-HTTP integration, not a guarantee that every
required authentication/delivery feature is enabled in your installed version.
See [Knative RabbitMQSource](https://knative.dev/docs/eventing/sources/rabbitmq-source/).

## JWT boundaries and authenticated event delivery

gRPC validates an RS256 caller token against the configured issuer, audience and
trusted JWKS endpoint, including expiry and subject. It accepts configured USER
or ADMIN roles from realm roles or the configured client. RecapManager already
forwards the caller JWT; its audience must also be valid for this API. Gateway
headers do not replace verification.

The HTTP event sink uses separate JWT trust settings and an explicit subject
allowlist for the delivery component. CloudEvent headers are not proof of sender
identity. A player token cannot be used as a delivery token merely because the
player can call the gRPC API. Health probes alone are unauthenticated, and their
routes should remain internal.

Configure the chosen delivery component to inject and refresh its own valid token,
or provide an authenticated platform forwarder. Do not assume a stock RabbitMQSource
supports that automatically: Knative's sender-identity compatibility list is
component-specific. The application fails closed if the configured token is not
provided. This integration must be checked before deploying; user JWTs must not
be added to queue bodies as a workaround. See
[Knative sender identity](https://knative.dev/docs/eventing/features/sender-identity/).

## NGINX and routing

Route calls through the Knative service URL or its configured gateway. Sending
requests directly to a revision pod or bypassing Knative with an ordinary
Kubernetes Service cannot activate a zero-replica application. External NGINX
must forward to the Knative route, preserve its host, the gRPC path and Bearer
metadata, and retain HTTP/2 rather than downgrading native gRPC to HTTP/1.1.

TLS can terminate at the trusted platform edge, with h2c inside the protected
network. If using an encrypted upstream, configure the controller's gRPC/TLS mode
accordingly. Do not rewrite gRPC method paths. Expose only the intended caller
route publicly; `/events/recap` and probes should remain internal. See
[NGINX gRPC routing](https://kubernetes.github.io/ingress-nginx/examples/grpc/).

The community `kubernetes/ingress-nginx` controller retired in March 2026; it is
distinct from F5's NGINX Ingress Controller. The cluster owner must identify which
one is installed and address its maintenance status. This application does not
change the existing controller. See the
[Kubernetes retirement statement](https://kubernetes.io/blog/2026/01/29/ingress-nginx-statement/).

## Timeouts, leases and retry recovery

Defaults bound provider calls to 30 seconds, each MinIO operation to 10 seconds,
and database connection/query waits to a few seconds. Event requests have a
120-second server limit and job leases last 180 seconds. Configure delivery and
Knative request timeouts to include event processing and cold-start time. Allow
at least 130 seconds for pod termination to cover the 125-second graceful shutdown.

gRPC validates `wait_millis` against a 5-second default maximum. Its polling budget
includes time already spent coordinating/publishing; dependency operations have
their own bounds. RecapManager's deadline remains the final caller-side bound.
A cold start can exceed it; RecapManager already maps deadline expiry to audio
`PENDING`. Queue generation continues through its separate event request.

Delivery retry/backoff must span a 180-second expired lease and subsequent work;
a few immediate retries cannot recover a dead replica. Use a retry window of at
least several minutes and preserve exhausted deliveries in a dead-letter store.
The application caps provider attempts at three by default. Duplicate deliveries
do not reset that count. Expired final attempts become `FAILED`, rather than
remaining pending forever. Keep replica clocks synchronized for UTC leases.

A failed job does not automatically charge the provider again on every lookup.
An operator can inspect the dead-letter event and failure, fix the cause, reset
the job under controlled maintenance, and replay the delivery. There is no public
reset endpoint. Lease tokens prevent an older worker from committing a newer
worker's metadata, but cannot undo a paid provider call after a crash. Size the
lease margin and Knative concurrency/maximum replicas for provider capacity.

## Audio URLs, retention and secrets

Metadata stores identity/status rather than an expiring URL. Every `READY` lookup
signs a fresh URL, valid for 900 seconds by default. Possession grants temporary
access to that object; do not publish signed URLs in logs.

`MINIO_ENDPOINT` is reachable from the application. `MINIO_PUBLIC_ENDPOINT` is the
browser-reachable origin used when signing; both must reach the same bucket.
Preserve the signed host, object path and query parameters through any proxy. Do
not replace the hostname after signing. Use HTTPS for frontend downloads. Audio
downloads bypass the TTS container, so playback does not require waking it.

Provision a private bucket and restricted service credentials. Coordinate object
expiration with metadata invalidation: deleting objects while retaining `READY`
rows produces broken audio links. Back up both stores. The metadata vault contains
supplied narrative text and needs player-data access/retention controls.

Runtime credentials are not embedded in the Docker image or build arguments.
Supply them using the deployment's secret mechanism. A future GitHub deployment
workflow should source values from repository secrets. The image build and tests
need no production credentials.

| Area | Required runtime values |
| --- | --- |
| Metadata | `MYSQL_URI`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` |
| Audio | `MINIO_ENDPOINT`, `MINIO_PUBLIC_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` |
| RabbitMQ | `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` |
| Caller JWT trust | `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_AUDIENCE`, `KEYCLOAK_JWK_SET_URI` |
| Event JWT trust | `TTS_EVENT_JWT_ISSUER`, `TTS_EVENT_JWT_AUDIENCE`, `TTS_EVENT_JWT_JWKS_URI`, `TTS_EVENT_JWT_SUBJECTS` (comma-separated exact subjects) |
| Synthesis | `ELEVEN_LABS_API_KEY` in default `elevenlabs` mode; `TTS_MOCK_AUDIO_FILE` in explicit `mock` mode |

Optional settings and defaults are in `application.properties`: `MYSQL_POOL_SIZE`
(4), `MINIO_BUCKET` (`tts-audio`), `MINIO_REGION` (`us-east-1`),
`AUDIO_URL_LIFETIME_SECONDS` (900), `RABBITMQ_PORT` (5672), `TTS_QUEUE`,
`RECAP_EVENTS_EXCHANGE`, `RECAP_EVENTS_ROUTING_KEY`,
`TTS_CONFIRM_TIMEOUT_MILLIS` (1000), `SPEECH_VOICE_ID`, `SPEECH_MODEL`,
`SPEECH_OUTPUT_FORMAT` (MP3 only), `TTS_MAX_CHARACTERS` (10000),
`TTS_MAX_WAIT_MILLIS` (5000), `TTS_MAX_ATTEMPTS` (3), `TTS_LEASE_SECONDS` (180),
`TTS_PROVIDER_TIMEOUT_SECONDS` (30), `TTS_MAX_AUDIO_BYTES` (20 MiB),
`TTS_PROVIDER_MODE` (`elevenlabs`), `TTS_PROVIDER_URL` (ElevenLabs HTTPS origin),
`KEYCLOAK_ROLE_CLIENT_ID`, `KEYCLOAK_USER_ROLE` (USER), `KEYCLOAK_ADMIN_ROLE` (ADMIN).
Knative supplies `PORT`.

Database capacity must cover maximum application replicas times pool size, plus
administration headroom; minimum idle connections are zero. Do not include `local`
in production `SPRING_PROFILES_ACTIVE`: it enables a direct development consumer
and mock audio fixture, bypassing the request-driven activation design.

## Verification and deployment boundary

For development without Kubernetes, follow [LOCAL_SETUP.md](LOCAL_SETUP.md).
MinIO and metadata MySQL belong to this service's Compose file. Local application
startup creates the missing private bucket and loads its bundled mock MP3; no
bootstrap container is required. Production still uses a pre-provisioned bucket
and restricted credentials. The root includes the service's Compose file alongside
RecapManager and the shared broker. The local
profiles use an offline JWT public key and fixtures; they do not test scale to zero.

Run `./mvnw.cmd clean verify` from this directory on Windows, or
`bash ./mvnw clean verify` on Linux/macOS. Unit, integration and connection tests
check content identity, duplicate generation, leases, crash recovery, bounded
attempts, cache misses, fresh URLs and caller/delivery authorization. Network tests
demonstrate native gRPC and HTTP on one port. External provider/audio responses
are substituted; database integration uses H2 in MySQL compatibility mode.

Deployment acceptance still requires real MySQL/MinIO/RabbitMQ connectivity and a
cluster test: let application replicas reach zero, deliver an event and a gRPC
cache miss, verify activation and persisted audio, then stop a processing replica
and verify recovery/redelivery. Verify signed-URL playback from the frontend and
that untrusted callers cannot access event delivery. No Kubernetes, ingress,
Knative or autoscaler manifests are created by this change.
