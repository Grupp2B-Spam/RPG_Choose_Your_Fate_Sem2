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
        SceneDocument document = new SceneDocument(
                event.sceneId(), event.version(), deleted ? null : event.title(),
                deleted ? null : event.text(), choices, deleted);

        // Atomic version predicate: older/repeated messages cannot overwrite a newer scene.
        Query onlyIfOlder = Query.query(Criteria.where("_id").is(event.sceneId())
                .and("version").lt(event.version()));
        Update update = new Update()
                .set("version", document.version())
                .set("title", document.title())
                .set("text", document.text())
                .set("choices", document.choices())
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
            if (event.title() == null || event.text() == null || event.choices() == null) {
                throw new IllegalArgumentException("UPSERT requires title, text, choices");
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
