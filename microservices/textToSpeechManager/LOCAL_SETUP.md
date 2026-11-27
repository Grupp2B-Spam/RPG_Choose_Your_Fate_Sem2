# Local RecapManager and TextToSpeechManager

Run these commands from the repository root unless a different directory is
specified. Docker Desktop must be running, and Java 21 or newer must be available.
This runs ordinary local processes with real MySQL, RabbitMQ and MinIO. It checks
the services together without simulating Knative activation or scaling to zero.

## Dependency ownership

| Dependency | Compose owner | Host address |
| --- | --- | --- |
| TextToSpeechManager application | `microservices/textToSpeechManager/compose.yaml` | HTTP/gRPC `localhost:9093` |
| Recap vault | `microservices/recapManager/docker-compose-recap-manager.yml` | `localhost:3307` |
| Speech metadata vault | `microservices/textToSpeechManager/compose.yaml` | `localhost:3313` |
| Audio MinIO | `microservices/textToSpeechManager/compose.yaml` | API `localhost:9002`, console `localhost:9003` |
| Bucket initialization and mock audio | TextToSpeechManager application, `local` profile | Startup initialization and bundled resource |
| Shared RabbitMQ | Root `docker-compose.yml` | AMQP `localhost:5672`, console `localhost:15672` |

The root Compose includes the owning service files, so these containers share one
network. TextToSpeechManager does not declare another broker or recap database.
The speech application and dependencies start with a normal `docker compose up -d`, both from the
repository root and from the speech service directory. These Compose files are
for running dependencies locally; Kubernetes deployment supplies external stores.

## Local credentials

When no environment overrides are supplied, the matching client/server defaults are:

| Dependency | Username | Password |
| --- | --- | --- |
| Shared RabbitMQ, including its management UI | `rabbitmq` | `rabbitmq` |
| Recap MySQL | `root` | `123456` |
| Speech MySQL application user | `tts_local` | `tts-local-development` |
| Speech MySQL administrator | `root` | `tts-root-local-development` |
| Speech MinIO | `local_minio` | `minio-local-development-only` |

These are local development defaults. Deployment still supplies its credentials
through secrets. For the shared broker, `RABBITMQ_USERNAME` is the preferred
environment variable; `RABBITMQ_USER` remains an alias. If both are supplied,
`RABBITMQ_USERNAME` wins in the broker and all its included service clients.
Use `RABBITMQ_PASSWORD` consistently. Actual `.env` files are not modified by code
changes: remove stale overrides or align them in the root and service directories.

### An existing RabbitMQ volume has different credentials

The broker persists user accounts in its volume. Changing Compose defaults and
recreating the container does not update those accounts. Preserve queued work
and update the local account using RabbitMQ's own administration command:

```powershell
docker compose up -d --wait rabbitmq
docker compose exec -T rabbitmq rabbitmqctl list_users
```

If `rabbitmq` is missing from that list:

```powershell
docker compose exec -T rabbitmq rabbitmqctl add_user rabbitmq rabbitmq
```

If the account already exists, update its password instead:

```powershell
docker compose exec -T rabbitmq rabbitmqctl change_password rabbitmq rabbitmq
```

Then grant the local account management access and permissions on the default
virtual host, and recreate the speech container with the aligned environment:

```powershell
docker compose exec -T rabbitmq rabbitmqctl set_user_tags rabbitmq administrator
docker compose exec -T rabbitmq rabbitmqctl set_permissions -p / rabbitmq '.*' '.*' '.*'
docker compose up -d --build --wait text-to-speech-manager
```

Log in at `http://localhost:15672` with `rabbitmq` / `rabbitmq`. Restart host Java
runners too so they load the updated properties. Do not delete database or broker
volumes to repair a password mismatch. See
[RabbitMQ user management and default-user behavior](https://www.rabbitmq.com/docs/access-control).

## Run TextToSpeechManager in Docker Desktop

Create the local JWT verification key once using the existing helper. Run from
the repository root; an existing key pair is reused:

```powershell
Push-Location microservices/recapManager
java tools/LocalToken.java player-1 ROLE_USER
Pop-Location
docker compose up -d --build --wait rabbitmq text-to-speech-manager
docker compose logs -f text-to-speech-manager
```

The speech container automatically starts its own MySQL and MinIO dependencies
and waits for them to become healthy. The image builds Java/protobuf sources with
the project's Maven wrapper inside Docker; no host Maven build or prebuilt JAR is
required. A normal root `docker compose up -d --build` also includes the speech
application alongside the other default services.

Stop any existing Maven/VS Code speech runner first so port 9093 is available.
Only the public verification key is mounted read-only into the container; the
private signing key and `.env` are not copied into the image. The helper's token
is needed for gRPC calls, not for RabbitMQ messages.

This Compose application always uses Spring's `local` profile: it initializes
the bucket, consumes RabbitMQ directly and returns the bundled mock audio. It
declares the agreed exchange, its queues and binding, so RecapManager need not be
running to publish a manual test event. Internal addresses use Docker service DNS;
returned audio URLs use browser-reachable `http://localhost:9002` by default.

Check HTTP readiness at `http://localhost:9093/health/ready`. Once the consumer is
connected, use RabbitMQ's management UI at `http://localhost:15672`, exchange
`choose-your-fate.recap`, routing key `recap.generated.v2`, and publish:

```json
{
  "eventId": "63c76472-747d-4ba8-9f8b-73950b94ab04",
  "eventType": "RecapGenerated",
  "eventVersion": 2,
  "characterId": 1,
  "requestType": "PATH_SUMMARY",
  "throughSequence": 1,
  "summaryVersion": 1,
  "text": "The hero reached the village safely."
}
```

Use `application/json` as the content type. The configured bucket will contain a
`speech/v1/<hash>.mp3` object after processing; gRPC returns a signed playback URL.
The default provider produces the test tone, not spoken narration. Set
`TTS_PROVIDER_MODE=elevenlabs` and provide `ELEVEN_LABS_API_KEY` in the service's
ignored `.env` to make real provider calls.

Docker Desktop shows the running application, queue handling and persistent
storage. It does not stop/start application replicas on demand: demonstrating
scale-to-zero requires the Knative activation and event-delivery infrastructure
documented in SERVERLESS_GUIDE.md. The same runtime image supports that deployment;
the image itself does not force the local profile.

To stop just the speech application, run
`docker compose stop text-to-speech-manager`. The sections below also cover host
Java execution when you want to debug in VS Code instead.

## Start dependencies for a host Java runner

The committed development defaults work without creating an environment file.
To override speech database or MinIO credentials, copy
`microservices/textToSpeechManager/local.env.example` to `.env` in that same
directory. Both Compose and the application's local profile read those names.
Use plain values without quotes or inline comments. Match any custom shared
broker credentials from the root `.env` in the applications' environment files.
Keep real deployment values in repository secrets and inject them at deployment;
these local defaults are only development substitutes.

```powershell
docker compose up -d --build --wait rabbitmq recap-vault tts-metadata-vault tts-minio
```

The first MinIO build compiles a pinned official source release and needs network
access. During its local startup, TextToSpeechManager creates the private
`tts-audio` bucket if missing and leaves an existing bucket and its policies
untouched. A concurrent creation by another instance of the same owner is safe.
Storage connection or permission failures stop startup instead of leaving the
application running with unusable storage.

The mock provider loads the bundled one-second test tone from
`classpath:mock-data/local-tone.mp3`. No bootstrap container, FFmpeg installation,
or generated `.local/fixture.mp3` is needed. Set `TTS_MOCK_AUDIO_FILE` to a custom
file path, `file:` URI or `classpath:` resource to use another fixture. This is a
playable test sound, not generated narration. Named volumes retain database and audio contents; changing
MySQL passwords in a file does not change an already initialized database.

To run the speech application's Compose file separately, first start the shared
broker with `docker compose up -d --wait rabbitmq` from the repository root.
Then, from the speech directory, set
`$env:TTS_RABBITMQ_HOST = 'host.docker.internal'` and run
`docker compose up -d --build`. This uses the broker's published host port instead
of expecting the separately created Compose networks to share service DNS. It
does not create another broker or RecapManager database.

If you previously ran the bootstrap container, it can remain as an exited orphan
until you remove it in Docker Desktop; it is no longer part of this setup.
The existing MinIO volume and stored audio remain in use.

## Start RecapManager

In a terminal whose working directory is `microservices/recapManager`:

```powershell
java tools/LocalToken.java player-1 ROLE_USER
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local,local-integration"
```

Save the printed Bearer token for the request below. The helper creates an ignored
local RSA key pair and reuses it on subsequent runs. RecapManager listens on 8090
and declares its RabbitMQ exchange. The speech local profile declares the same
exchange, so either application can be started first.

RecapManager's local profile already selects the real speech gRPC client at
`localhost:9093` with local plaintext transport. Those settings live in its own
`application-local.properties`; do not set PowerShell variables to enable the
connection. TextToSpeechManager's Docker port and local server already match.
When RecapManager runs in Docker instead, its own Compose configuration selects
`text-to-speech-manager:9093` on the shared network automatically.

The opt-in `local-integration` profile replaces only the external language model
with a deterministic fixture that invokes the existing AI tools. Journey and
ownership retrieval use local fixtures; normal persistence, outbox publishing,
JWT validation and speech gRPC calls remain enabled. To exercise your configured
real model, use only `local` instead. Ensure any existing `.env` overrides match
the database and broker credentials you started.

## Alternative: start TextToSpeechManager through Maven or VS Code

Use this instead of the speech application container when debugging. Stop the
container first with `docker compose stop text-to-speech-manager` from the root;
leave its database, MinIO and the shared broker running.

In a second terminal whose working directory is
`microservices/textToSpeechManager`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Its local defaults use port 9093, the service-owned dependencies above and the
public key at `../recapManager/.local/recap-jwt-public.pem`. JWT signature,
expiration, issuer and audience checks remain active. An independently generated
key or token with different issuer/audience will not work. The local RabbitMQ
consumer creates the matching exchange, its queues and binding; no event-delivery
platform is required. A custom audio file path configured for a container must
be accessible inside that container through an explicitly configured mount;
host file paths are not automatically copied into the image.

For VS Code's runner, use each service directory as its working directory and
select the same profiles. No speech connection environment variables are needed
on the RecapManager launch configuration.

When the whole repository is open in VS Code, use Run and Debug and select
`RecapManager (local integration)`, then press F5. The root workspace launch entry
sets both the service working directory and `local,local-integration` profiles.
An entry is also available when opening only the recapManager folder. The
existing service-folder launch entry using only `local` retains the configured
real language model.

If the log says `No active profile set` followed by a missing
`KEYCLOAK_ISSUER_URI` error, the local launch entry was not used. The default
profile requires deployment identity settings. Running with the repository root
as the working directory also prevents the local profile from finding the
service-relative public verification key.

## Request a summary with audio

In a third PowerShell terminal, paste the token printed by the helper:

```powershell
$token = Read-Host 'Paste the local JWT'
$headers = @{ Authorization = "Bearer $token"; 'X-API-VERSION' = '2' }
$body = @{ characterId = 1; requestType = 'PATH_SUMMARY'; audio = $true } | ConvertTo-Json
$result = Invoke-RestMethod -Method Post -Uri 'http://localhost:8090/choose-your-fate/ai/ask' -Headers $headers -ContentType 'application/json' -Body $body
$result
```

The response contains the summary and audio status. If `PENDING`, repeat the same
request: waiting is bounded while generation continues. Once `READY`, open the
returned audio URL to hear the fixture. Repeating the request with unchanged
journey data reuses the saved summary and content-addressed audio. Signed URLs
may change. Tokens expire after 15 minutes; rerun `LocalToken.java` to refresh.

## Direct gRPC request

With both applications running as above, you can call speech directly using
grpcurl's Docker image. Run from the repository root:

```powershell
Push-Location microservices/recapManager
$token = java tools/LocalToken.java player-1 ROLE_USER
Pop-Location
$protoDirectory = (Resolve-Path 'microservices/textToSpeechManager/src/main/proto').Path
$body = @{ text = 'The hero reached the village safely.'; waitMillis = 5000 } | ConvertTo-Json
$body | docker run --rm -i `
  --mount "type=bind,source=$protoDirectory,target=/protos,readonly" `
  ghcr.io/fullstorydev/grpcurl:latest `
  -plaintext -import-path /protos -proto speech/v1/speech.proto `
  -H "authorization: Bearer $token" -d '@' `
  host.docker.internal:9093 `
  choose_your_fate.speech.v1.SpeechService/GetOrCreateSpeech
```

The caller supplies text and a bounded wait only. TextToSpeechManager owns
`SPEECH_VOICE_ID`, `SPEECH_MODEL` and `SPEECH_OUTPUT_FORMAT` and returns its computed
`contentHash`. Repeat a pending call to retrieve the completed fixture audio.
If those settings change, new calls use a new audio identity. Already queued
speech jobs retain their stored settings.

Restart both applications after this contract update. Remove any explicit
`RECAP_EVENTS_ROUTING_KEY=recap.generated.v1` override from your local environment,
or update both services to `recap.generated.v2`. Existing queued version 1 recap
events can still be consumed; their old synthesis fields are ignored. Existing
version 1 internal speech events resolve the previously stored job by hash.

## Stop

Stop the two Java runners with Ctrl+C, then stop only these containers:

```powershell
docker compose stop text-to-speech-manager tts-minio tts-metadata-vault recap-vault rabbitmq
```

Leave `rabbitmq` or `recap-vault` out if another running process still needs them.
Root `docker compose down` stops the other services in the combined project too.
