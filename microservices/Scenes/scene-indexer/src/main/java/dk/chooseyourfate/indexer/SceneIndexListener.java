package dk.chooseyourfate.indexer;

import java.nio.charset.StandardCharsets;
import java.util.List;
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

        boolean deleted = "DELETE".equals(event.operation());
        List<Choice> choices = deleted ? List.of() : event.choices();
        String name = deleted ? null : event.name() != null ? event.name() : event.title();
        String description = deleted ? null
                : event.description() != null ? event.description() : event.text();
        String status = deleted ? "DELETED"
                : event.status() != null ? event.status() : "PUBLISHED";
        SceneDocument document = new SceneDocument(
                event.sceneId(), event.sceneId(), event.version(), event.chapterId(),
                status, name, description, choices,
                event.npcIds() == null ? List.of() : event.npcIds(),
                event.questIds() == null ? List.of() : event.questIds(), deleted);

        // Atomic version predicate: older/repeated messages cannot overwrite a newer scene.
        Query onlyIfOlder = Query.query(Criteria.where("_id").is(event.sceneId())
                .and("version").lt(event.version()));
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
        if (event.sceneId() == null || event.sceneId().isBlank() || event.version() < 1) {
            throw new IllegalArgumentException("sceneId and positive version are required");
        }
        if (!"UPSERT".equals(event.operation()) && !"DELETE".equals(event.operation())) {
            throw new IllegalArgumentException("operation must be UPSERT or DELETE");
        }
        if ("UPSERT".equals(event.operation())) {
            if ((event.name() == null && event.title() == null)
                    || (event.description() == null && event.text() == null)
                    || event.choices() == null) {
                throw new IllegalArgumentException(
                        "UPSERT requires name, description, and choices");
            }
            for (Choice choice : event.choices()) {
                if (choice == null || choice.id() == null || choice.id().isBlank()
                        || choice.text() == null) {
                    throw new IllegalArgumentException("invalid choice");
                }
            }
        }
    }
}
