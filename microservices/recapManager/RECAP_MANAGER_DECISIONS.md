# RecapManager architecture decisions

This log records RecapManager decisions, their reasons and consequences. Add a
dated entry when a decision is accepted, changed or superseded. Proposals remain
explicitly marked until agreed. Dates use Europe/Copenhagen time.

The entries below were recorded on 2026-11-27 from decisions already agreed during
the service's development; this is the recording date, not a new approval date.
TextToSpeechManager's internal decisions are recorded separately in
[TEXT_TO_SPEECH_MANAGER_DECISIONS.md](../textToSpeechManager/TEXT_TO_SPEECH_MANAGER_DECISIONS.md).

## 2026 11 27 Service boundary and incremental narratives

Status: accepted, carried forward. RecapManager owns saved summaries/recaps and
their processing checkpoints in its own MySQL recap vault. It does not use the
monolith's secondary datasource or own raw character path choices. Retrieve new
journey entries from StoryPathManager over gRPC using the saved checkpoint.

Combine new entries with the saved narrative, then replace the saved narrative
and advance its checkpoint only after successful generation. If no new entries
exist, return the saved narrative without regeneration or a narrative write.
Every successful request returns the requested narrative, updated first when
needed. This avoids repeated processing of already summarized choices.

## 2026 11 27 HTTP endpoint and versioning

Status: accepted, carried forward. Run on port 8090. Keep
`POST /choose-your-fate/ai/ask` and declare supported versions on endpoint methods,
selected through `X-API-VERSION`. Version 1 preserves text responses; version 2
adds optional audio. Use the existing controller rather than a second controller
for the same recap flow. Endpoint versions are not governed by one global API
version number.

## 2026 11 27 Preserve usable implementation

Status: accepted, carried forward. Adapt existing services and AiTools methods to
the new retrieval and persistence flow. Replace code only where it is no longer
usable; avoid unnecessary rewrites across the project. This preserves established
behavior and makes the migration easier to review.

## 2026 11 27 Caller identity and character access

Status: accepted, carried forward. Validate caller JWTs in the microservice even
when requests pass through a gateway. Forward the caller's token on outgoing gRPC
queries. Do not put user JWTs in RabbitMQ events. Issuer, audience and role mapping
come from deployment configuration rather than guessed identity-provider values.

Character ownership is checked through the CharacterManager lookup boundary,
with retrieval mocked while that integration is unavailable. Unknown ownership
must not grant access. This keeps authorization tied to current ownership rather
than stale character IDs embedded in a token.

## 2026 11 27 Speech integration and publication

Status: accepted, carried forward. Begin queued narration with `PATH_SUMMARY`, the
character summary used by the frontend. Publish changed summary work through real
RabbitMQ. Persist the publication intent with the narrative so a committed change
does not silently lose its event.

Query TextToSpeechManager through gRPC for cached or missing audio. Wait within a
bounded timeout and report `PENDING` if it is still generating. Reuse audio by the
existing content hash, which includes exact text, voice, model and output format.
Keep the voice/model defaults consistent with the old backend. TextToSpeechManager
owns audio generation/storage; RecapManager does not implement those internals.

## 2026 11 27 External substitutes and meaningful tests

Status: accepted, carried forward. Mock outgoing data retrieval until other teams'
services are available; do not retrieve from the monolith or create their services
as substitutes. Application RabbitMQ publishing remains real.

Group tests into unit, integration and connection tests. Focus on this service's
business rules and integration contracts. Improve weak assertions or remove
redundant checks rather than retaining tests that only prove framework behavior
or mock setup. Tests using a mocked RabbitTemplate do not prove broker connectivity.

## 2026 11 27 Version gated Docker Hub publication

Status: accepted, carried forward. Publish a Docker image when RecapManager's own
project version in `pom.xml` changes on `main`, after tests pass. Pull requests and
manual runs validate without publishing. Use the shared `DOCKERHUB_USERNAME` and
`DOCKERHUB_TOKEN` repository secrets with a separate `recap-manager` image.

Runtime credentials stay outside the image and are supplied at deployment. The
workflow tests, builds and publishes; it does not deploy the running service.

## 2026 11 27 Architecture diagram scope

Status: accepted. Apply architecture updates only to the working duplicate,
`architecture-chooseYourFate-revision-level-2.drawio`. Keep the original Patrick
diagram unchanged as a reference unless explicitly instructed otherwise.

## 2026 11 27 Local integration ownership

Status: accepted by the user. Keep the recap vault in this service's existing
Compose file. Speech MinIO and metadata infrastructure belong exclusively in
TextToSpeechManager's Compose file, included by the root alongside this service.
Reuse the root's shared RabbitMQ broker rather than defining a second local stack.

For local integration, the opt-in combination of local and local-integration
profiles supplies a fixture language model that invokes the existing AI tools.
It keeps persistence and publishing real while avoiding an external LLM dependency.
The ordinary local profile still uses the configured model. Neither profile is a
production deployment configuration. See ../textToSpeechManager/LOCAL_SETUP.md.

## 2026 11 27 Speech configuration belongs to TextToSpeechManager

Status: accepted by the user. Remove voice/model/output-format settings and audio
hash calculation from RecapManager. Its speech gRPC request sends only the saved
narrative and bounded wait; TextToSpeechManager chooses its configured synthesis
settings and returns the audio identity. Forward that identity in the existing
HTTP version 2 response. If no speech response arrives, contentHash is null;
RecapManager cannot know the remote audio identity in advance.

The transactional outbox still commits with the narrative. RecapGenerated version
2 sends the saved text, character, request type, checkpoint and summary version,
without synthesis settings or an audio hash. Default routing is recap.generated.v2.
Existing pending outbox payloads are not rewritten; the new TTS consumer accepts
older queued recap events while ignoring their superseded synthesis fields.
Deploy/update both services together and align any routing-key overrides.

Keep the existing request helper, client, AI tools, persistence and outbox flow;
change only the parts that previously decided the speech service's configuration.

## 2026 11 27 Shared local broker credentials

Status: requested by the user. Align broker credentials in the base/local Spring
properties and this service's Compose file with the shared broker's local account:
rabbitmq/rabbitmq. Prefer RABBITMQ_USERNAME, falling back to the legacy
RABBITMQ_USER alias, with identical precedence for all shared-broker clients.
Database defaults already match their owning vaults and remain unchanged.
Existing RabbitMQ accounts require an explicit non-destructive password/account
update; changing container environment values alone does not migrate its volume.

## 2026 11 27 Self-contained local speech connection

Status: requested by the user. RecapManager's local Spring profile declares the
actual speech gRPC client, localhost:9093 target and plaintext local transport in
application-local.properties. Starting the service in VS Code or through Maven
must not require SPEECH_MODE, SPEECH_GRPC_TARGET or SPEECH_GRPC_PLAINTEXT shell
variables. TextToSpeechManager already publishes the matching local port.
The local-integration profile substitutes only the language model; the local
speech connection is real. Other teams' retrieval fixtures remain in place.
The service-owned local Compose file likewise selects the real client and local
plaintext transport, using text-to-speech-manager:9093 on the Docker network.
Optional deployment overrides remain supported; they are not prerequisites for
the two supported local execution paths.

## 2026 11 27 Explicit VS Code local launch configuration

Status: implementation fix for the reported startup failure. The VS Code launch
must select local,local-integration and use recapManager as its working directory
when the full repository is open. A nested .vscode configuration is not the root
workspace's launch configuration. Provide the named RecapManager (local integration)
entry in both workspace layouts, while preserving the existing local-only entry
for the configured real model. IDE launch files remain local ignored settings.

Do not change the deployment/default security profile to bypass its missing
issuer configuration. The logged failure was a launch-profile selection problem;
the database connection and JPA initialization had already succeeded.
