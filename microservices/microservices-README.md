# Microservices

- [Recap Manager](recapManager/) generates recaps and optional narration.
- [TextToSpeechManager](textToSpeechManager/README.md) generates reusable audio with Knative activation, MinIO audio storage and a MySQL metadata vault.
- [AccountManager](accountManager/README.md) owns identity and JWT issuance in the architecture. Its scaffold publishes the public verification key via SOAP.

Each service keeps its own architecture decisions and reasons:

[Run RecapManager and TextToSpeechManager locally](textToSpeechManager/LOCAL_SETUP.md).
Local dependencies are declared in the owning service's Compose file and included
by the root; RabbitMQ is shared root infrastructure.
The shared broker and its clients default locally to `rabbitmq` / `rabbitmq`.
Use `RABBITMQ_USERNAME` and `RABBITMQ_PASSWORD` for overrides; `RABBITMQ_USER`
remains a lower-priority username alias. The local setup guide documents all
store credentials and how to repair accounts in an existing RabbitMQ volume.

- [RecapManager decisions](recapManager/RECAP_MANAGER_DECISIONS.md)
- [TextToSpeechManager decisions](textToSpeechManager/TEXT_TO_SPEECH_MANAGER_DECISIONS.md)
# Add microservices to this folder.

CharacterManager -> Mikkel
AccountManager -> Alexander
StoryContentManager -> Patrick
RecapManager -> Sebastian
SceneIndexer -> Markus
SceneManager -> Markus
