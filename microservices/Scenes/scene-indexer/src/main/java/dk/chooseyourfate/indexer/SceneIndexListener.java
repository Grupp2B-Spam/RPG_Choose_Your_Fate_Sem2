package dk.chooseyourfate.indexer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;


@Component
public class SceneIndexListener {
    private final MongoTemplate mongo;
    private final JsonMapper json;

    public SceneIndexListener(MongoTemplate mongo, JsonMapper json) {
        this.mongo = mongo;
        this.json = json;
    }

    @RabbitListener(queues = "scene-indexer.scene-changed")
    public void onSceneChanged(Message message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        SceneChangedEvent event = json.readValue(body, SceneChangedEvent.class);
        validate(event);

        SceneChangedEvent.ScenePayload payload = event.payload();
        boolean deleted = "RETIRED".equals(payload.status());
        List<Choice> choices = deleted ? List.of() : payload.choices().stream()
                .map(choice -> new Choice(choice.choiceId(), choice.label(),
                        choice.destinationSceneId(), Map.of()))
                .toList();
        SceneDocument document = new SceneDocument(
                payload.sceneId(), payload.sceneId(), payload.sceneVersion(), payload.chapterId(),
                payload.status(), deleted ? null : payload.name(),
                deleted ? null : payload.description(), choices,
                payload.npcIds() == null ? List.of() : payload.npcIds(),
                payload.questIds() == null ? List.of() : payload.questIds(), deleted);

        // Atomic version predicate: older/repeated messages cannot overwrite a newer scene.
        Query onlyIfOlder = Query.query(Criteria.where("_id").is(payload.sceneId())
                .and("version").lt(payload.sceneVersion()));
        Update update = new Update()
                .set("sceneId", document.sceneId())
                .set("version", document.version())
                .set("chapterId", document.chapterId())
                .set("status", document.status())
                .set("name", document.name())
                .set("description", document.description())
                .set("choices", document.choices())
                .set("npcIds", document.npcIds())
                .set("questIds", document.questIds())
                .set("deleted", document.deleted());

        var result = mongo.updateFirst(onlyIfOlder, update, SceneDocument.class);
        if (result.getMatchedCount() > 0) return;

        // New scene: insert. If another worker inserted meanwhile, retry the guarded update.
        try {
            mongo.insert(document);
        } catch (DuplicateKeyException alreadyExists) {
            mongo.updateFirst(onlyIfOlder, update, SceneDocument.class);
        }
    }

    private static void validate(SceneChangedEvent event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("scene-content.updated payload is required");
        }
        SceneChangedEvent.ScenePayload payload = event.payload();
        if (!"scene-content.updated".equals(event.eventType()) || event.schemaVersion() < 1) {
            throw new IllegalArgumentException("unexpected event type or schema version");
        }
        if (payload.sceneId() == null || payload.sceneId().isBlank()
                || payload.sceneVersion() < 1) {
            throw new IllegalArgumentException("sceneId and positive sceneVersion are required");
        }
        if (!"PUBLISHED".equals(payload.status()) && !"RETIRED".equals(payload.status())) {
            throw new IllegalArgumentException("status must be PUBLISHED or RETIRED");
        }
        if ("PUBLISHED".equals(payload.status())) {
            if (payload.name() == null || payload.description() == null
                    || payload.choices() == null) {
                throw new IllegalArgumentException(
                        "PUBLISHED scene requires name, description, and choices");
            }
            for (SceneChangedEvent.ContractChoice choice : payload.choices()) {
                if (choice == null || choice.choiceId() == null
                        || choice.label() == null || choice.destinationSceneId() == null) {
                    throw new IllegalArgumentException("invalid choice");
                }
            }
        }
    }
}
