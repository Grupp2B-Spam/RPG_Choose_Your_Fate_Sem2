# TextToSpeechManager architecture decisions

This log records TextToSpeechManager decisions, their reasons and consequences.
Add a dated entry when a decision is accepted, changed or superseded. Proposals
remain explicitly marked until agreed. Dates use Europe/Copenhagen time.
RecapManager's own decisions are recorded separately in
[RECAP_MANAGER_DECISIONS.md](../recapManager/RECAP_MANAGER_DECISIONS.md).

## 2026 11 27 TextToSpeechManager boundary

Status: accepted. TextToSpeechManager is a separate microservice under
`microservices/`, independent of the monolithic backend. It owns speech generation
and reusable audio. RecapManager continues to own narratives and their checkpoints.
This keeps the two services independently deployable and avoids retrieving
narratives from the old backend.

## 2026 11 27 Speech integration contracts

Status: accepted, preserving the existing implementation. RabbitMQ is the primary
generation trigger. The existing `choose_your_fate.speech.v1.SpeechService`
`GetOrCreateSpeech` RPC retrieves cached speech or requests missing speech. It waits
within the requested bound, then returns `PENDING` if generation is unfinished.
`READY` includes an audio URL. The wire contract remains compatible with RecapManager.

Audio identity includes the exact text, voice, model and output format using the
existing length-prefixed SHA-256 algorithm. A narrative change therefore creates a
new identity, while repeated requests for the same version reuse its audio.

## 2026 11 27 Service authentication

Status: accepted, carrying forward the existing service rule. HTTP and gRPC business
requests validate caller JWTs. Queue events do not contain user JWTs; RabbitMQ
credentials and network access define the queue trust boundary. JWT issuer,
audience and keys are deployment configuration, not invented service constants.

## 2026 11 27 Serverless deployment scope

Status: accepted. Prepare the service to run in a Kubernetes cluster with the
existing NGINX ingress controller. Do not create Kubernetes, ingress or autoscaler
manifests in this change. Explain activation, persistence, protocols, configuration
and operational limitations in a guide separate from this decision log.

## 2026 11 27 Outgoing dependency substitutes

Status: accepted, carrying forward the existing development rule. Tests and local
development can substitute external data and provider responses. Production
publishing uses actual RabbitMQ. No other team's microservice is created as part
of TextToSpeechManager.

## 2026 11 27 Whole application scales to zero with Knative

Status: accepted by the user. Use Knative to scale the entire TextToSpeechManager
application to zero, retaining the existing gRPC protocol. The alternative of an
always-running gRPC API with KEDA-scaled generation workers was considered and not
selected. Idle application instances can stop; RabbitMQ, the event-delivery
infrastructure, MinIO, MySQL and the Knative activation infrastructure remain
available. Cold starts can cause an initial lookup to time out and be reported by
RecapManager as pending.

Implementation: serve gRPC, authenticated CloudEvent delivery and health checks on
one HTTP/2-capable port, honoring Knative's `PORT`. Armeria supplies this shared
listener while Spring Boot supplies dependency injection and configuration. There
is no scheduled worker or permanent RabbitMQ consumer in the default profile.
Generation completes inside an event request before that request succeeds.

## 2026 11 27 Local dependency ownership

Status: accepted by the user. Define every local dependency in its owning
microservice's existing Compose file. TextToSpeechManager owns MinIO, its metadata
MySQL vault. Local bucket initialization and the audio fixture are handled by
the application itself, as recorded below. RecapManager keeps its own
vault; RabbitMQ remains shared infrastructure in the root Compose file. The root
includes the service files to provide one network. Do not create a separate local
Compose stack or duplicate another service's database or the shared broker.

Start the service-owned dependencies with ordinary `docker compose up -d`; do not
require a separate Compose profile. This replaces the initial profile restriction,
which caused normal startup to skip the entire speech dependency setup. Keep
application development defaults restricted to Spring's local profile.
Local speech verifies RecapManager's offline RSA JWTs and uses a playable
MP3 fixture; real storage and publishing still run. Production keeps its configured
identity provider and provider credentials. See LOCAL_SETUP.md for the runnable
commands and runtime.env.example for deployment environment names.

## 2026 11 27 MinIO audio and separate MySQL metadata vault

Status: accepted by the user. Keep audio files in MinIO as shown in the Patrick
diagram and add a service-owned MySQL metadata vault. Store content identities,
generation state, attempt counts and worker leases in MySQL; do not store audio
bytes in the database. Atomic conditional row updates coordinate concurrent
instances more simply than object-only metadata. The cost is a database that
needs credentials, backups and connection capacity even when the application is
scaled to zero. Only `architecture-chooseYourFate-revision-level-2.drawio`, the
working duplicate, is updated to show the separate stores. Keep the original
Patrick revision unchanged as a reference; future diagram edits target the
working duplicate unless explicitly requested otherwise.

The audio upload and metadata commit are separate operations. Retrying a job
checks its deterministic MinIO object key before calling ElevenLabs, recovering
an upload that succeeded before the instance stopped. Expired leases can be
reclaimed, and lease tokens prevent an older worker from completing a newer
worker's metadata. This provides reusable results and bounded retries, not an
exactly-once guarantee for billed provider calls.

## 2026 11 27 Event delivery and local development

Status: implementation decision within the accepted serverless design. A
RabbitMQ-to-HTTP delivery component must retain work until the sink succeeds,
perform bounded retries and dead-letter failed deliveries. Its HTTP request uses
its own verified JWT with an explicitly allowed subject. User JWTs are never
stored in or read from queue messages. A stock event source must not be assumed
to support authenticated delivery without verifying its capabilities; this is a
cluster integration prerequisite.

The `local` profile provides a direct RabbitMQ consumer for development without
Knative and uses an explicit MP3 fixture in mock provider mode. This profile must
not be used in Knative, because an in-process queue subscription cannot activate a
container that has scaled to zero. Default production synthesis uses ElevenLabs.

## 2026 11 27 TTS owns synthesis configuration and audio identity

Status: accepted by the user. Voice, model and output format are configured only
by TextToSpeechManager using SPEECH_VOICE_ID, SPEECH_MODEL and
SPEECH_OUTPUT_FORMAT. HTTP callers of RecapManager and gRPC callers of this service
do not choose these settings. The gRPC request carries text and the bounded wait;
this service constructs the synthesis specification and computes its audio hash.
The response includes that hash, status and a fresh URL when ready.

RecapGenerated version 2 contains text and recap metadata. Its default routing key
is recap.generated.v2. Older queued version 1 recap payloads are read as text only;
their synthesis options and hash do not override this service's configuration.
Both the event and gRPC paths use the same configured specification, so duplicate
requests reuse the same audio. Changing text or any synthesis setting produces a
new identity without changing the hash algorithm or invalidating old audio files.

Internal SpeechRequested version 2 contains a job hash only. The specification is
persisted before publication and loaded from MySQL for processing, including for
older queued internal events. Thus queued jobs retain their original settings
across configuration changes. No new database schema or data migration is needed.
The existing leases, retries, provider adapter and object storage flow are reused.

## 2026 11 27 Application-owned local initialization

Status: accepted by the user. Remove the separate tts-local-bootstrap container
and its Python/FFmpeg setup. The local application creates the configured MinIO
bucket if missing before exposing its endpoints or consuming messages. It leaves
existing buckets, audio and access policies unchanged. Concurrent creation of an
already-owned bucket is safe; connection or permission failures fail startup.

The mock provider reads a bundled synthetic one-second 440 Hz MP3 fixture instead
of requiring a generated file on disk. Custom TTS_MOCK_AUDIO_FILE values still
support plain file paths, file URIs and classpath resources. Neither FFmpeg nor
Python is required by application startup. The fixture was generated using
FFmpeg's sine source at 44100 Hz and encoded with libmp3lame at 128 kbps.

Only Spring's local profile creates the bucket. Production/Knative continues to
use pre-provisioned private storage and restricted credentials, avoiding bucket
administration permissions and provisioning work during serverless cold starts.

## 2026 11 27 Local application container in the owning Compose file

Status: accepted by the user. Run TextToSpeechManager in Docker Desktop through
its existing Compose file, included by the root. Start the application by default
without a special Compose profile. Keep the speech stores in that same file and
reuse the root's shared RabbitMQ. The application waits for its MySQL and MinIO
health checks, uses internal Docker DNS, and signs URLs against the configured
browser-reachable MinIO origin. Expose its HTTP/gRPC port on localhost:9093.

The Compose application uses the local Spring profile with mock provider audio
by default. Its local RabbitMQ topology declaration includes the matching
publisher-owned exchange contract, so manual queue tests do not depend on a
running RecapManager. Production still uses external event-delivery topology.
Mount only the local public JWT verification key read-only. Runtime settings are
injected as environment values; no credentials or private keys enter the image.

Build the application and generated protobuf sources inside a separate Docker
build stage using the project's pinned Maven wrapper. This removes the host-JAR
prerequisite. The runtime image keeps its Java 21 non-root execution and accepts
Knative's PORT without forcing a local profile. Tests remain a separate verify
step. No Docker Hub publication or Kubernetes manifests are introduced here.

Docker Desktop demonstrates local processing and service integration, not
Knative activation or scale-to-zero. That behavior requires the cluster
prerequisites in SERVERLESS_GUIDE.md; an ordinary Compose restart policy is not a
replacement for the serverless activation infrastructure.

## 2026 11 27 Shared local broker credentials

Status: requested by the user. Use rabbitmq/rabbitmq as the no-environment local
broker account across the root broker, this service, RecapManager and the included
StoryContentManager client. Prefer RABBITMQ_USERNAME and support RABBITMQ_USER as
a lower-priority alias everywhere. Leave production's required runtime credentials
and other service-owned database/storage accounts unchanged; their client and
server defaults already match. Document the full local credential table.

Persisted RabbitMQ users do not change when Compose defaults change. Repair an
existing local account with rabbitmqctl rather than clearing its volume. Do not
overwrite actual environment files or reset queues as part of this config change.

## 2026 11 27 Local gRPC connection without shell setup

Status: requested by the user. RecapManager's local profile contains its own speech
client settings and connects to this service on the published localhost:9093
port. Running the two services locally must not depend on manually injecting
three SPEECH_* connection variables. This service's Docker port mapping and
HTTP/2 cleartext listener already provide the receiving side of that connection.
