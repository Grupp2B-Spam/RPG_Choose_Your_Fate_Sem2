# TextToSpeechManager

A separate Java 21 service for reusable speech. It implements RecapManager's gRPC
wire contract, consumes narration work through request-based CloudEvent delivery,
stores audio in MinIO, and coordinates generation through its own MySQL metadata
vault. The whole application can scale to zero when deployed with Knative.

- [Architecture decisions and reasons](TEXT_TO_SPEECH_MANAGER_DECISIONS.md)
- [Serverless operation and cluster integration guide](SERVERLESS_GUIDE.md)
- [Metadata schema](src/main/resources/schema.sql)
- [gRPC contract](src/main/proto/speech/v1/speech.proto)

## Build and verify

From this directory:

```powershell
.\mvnw.cmd clean verify
docker build -t text-to-speech-manager:local .
```

Linux/macOS: `bash ./mvnw clean verify`. The Dockerfile builds the JAR from source
in a separate build stage with the project's pinned Maven wrapper; tests run
separately with `clean verify`. The runtime runs as a non-root user and supports Knative's `PORT`; the ordinary local
default is 9093. Docker requires a running Docker engine.

Tests are grouped into `unit`, `integration` and `connection`. `test` runs only
unit tests; `verify` runs all groups. External provider/audio responses are
substituted; database integration uses H2 in MySQL compatibility mode. The network
tests start real local HTTP/gRPC listeners. They do not prove connectivity to a
running RabbitMQ, MinIO, MySQL, identity provider or Kubernetes cluster.

## Run locally

Follow [the local setup guide](LOCAL_SETUP.md) to run this service and RecapManager
together. This service's [Compose file](compose.yaml) runs the application, MinIO and the speech
metadata database. The application creates its missing bucket during local
startup and loads a bundled MP3 fixture. RecapManager's Compose file
owns its vault; the root Compose file owns the shared RabbitMQ broker and includes
both service files. There is no separate local Compose stack.

After creating the local public verification key, run
`docker compose up -d --build --wait rabbitmq text-to-speech-manager` from the
repository root. The application starts on port 9093 and uses Docker service DNS
for its stores and broker. See the guide for JWT setup and a RabbitMQ test message.
This runs the service in Docker Desktop; Knative is still needed for scale-to-zero.

The `local` profile uses the bundled MP3 fixture and the same offline JWT public
key as RecapManager. Publishing and storage remain real. Default/production mode
uses external configuration and authenticated event delivery. Never enable the
local profiles in Knative. [runtime.env.example](runtime.env.example) documents
the production settings to supply from repository secrets at deployment; they
are not baked into the image or required by the image build workflow.

## Public contracts

| Transport | Address | Authentication |
| --- | --- | --- |
| gRPC | `choose_your_fate.speech.v1.SpeechService/GetOrCreateSpeech` | Caller Bearer JWT with configured USER or ADMIN role |
| HTTP event delivery | `POST /events/recap` | Delivery-component Bearer JWT with an explicitly allowed subject |
| HTTP probes | `GET /health/live`, `GET /health/ready` | No JWT; keep these internal |

There is no second REST speech API. Native gRPC and event/probe HTTP requests share
one port, including HTTP/2 cleartext support behind a trusted Knative proxy.

The request contains only `text` and `wait_millis`. TextToSpeechManager selects
the voice/model/MP3 format from its own environment configuration and calculates
the audio hash. A cache hit returns `READY` with that hash and a newly signed MinIO URL. A miss publishes
durable work, waits within `wait_millis`, then returns `PENDING` or the completed
result. Generation failures eventually return `FAILED`; dependencies that cannot
accept work return gRPC `UNAVAILABLE`.

RecapManager publishes `RecapGenerated` version 2 with the narrative and its
character/checkpoint/version metadata. It does not supply synthesis settings or an
audio hash. TextToSpeechManager's internal `SpeechRequested` version 2 references
a job hash; the job's settings are already stored in its metadata vault.
