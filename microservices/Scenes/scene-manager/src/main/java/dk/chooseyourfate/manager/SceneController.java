package dk.chooseyourfate.manager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/scenes")
public class SceneController {
    private final SceneRepository scenes;
    private final RabbitTemplate rabbit;
    private final JsonMapper json;

    public SceneController(SceneRepository scenes, RabbitTemplate rabbit, JsonMapper json) {
        this.scenes = scenes;
        this.rabbit = rabbit;
        this.json = json;
    }

    @GetMapping("/{id}")
    public SceneView getScene(@PathVariable String id) {
        return toView(requireScene(id));
    }

    @GetMapping("/{id}/nextscenes")
    public List<NextScene> getNextScenes(@PathVariable String id) {
        SceneDocument scene = requireScene(id);
        List<String> targetIds = scene.choices().stream()
                .map(Choice::nextSceneId)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        Map<String, SceneDocument> found = scenes.findAllById(targetIds).stream()
                .filter(target -> !target.deleted())
                .collect(Collectors.toMap(SceneDocument::id, Function.identity()));
        return targetIds.stream()
                .filter(found::containsKey)
                .map(targetId -> new NextScene(targetId, found.get(targetId).title()))
                .toList();
    }

    @PostMapping("/{id}/choices/{choiceId}")
    public ResponseEntity<ChoiceReceipt> choose(
            @PathVariable String id,
            @PathVariable String choiceId,
            @RequestBody ChooseRequest request) {
        if (request == null || request.characterId() == null || request.characterId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "characterId is required");
        }
        SceneDocument scene = requireScene(id);
        Choice choice = scene.choices().stream()
                .filter(candidate -> choiceId.equals(candidate.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choice does not belong to this scene"));

        String eventId = UUID.randomUUID().toString();
        ChoiceMadeEvent event = new ChoiceMadeEvent(
                eventId, request.characterId(), scene.id(), scene.version(),
                scene.title(), scene.text(), choice.id(), choice.text(),
                choice.nextSceneId(), choice.effects() == null ? Map.of() : choice.effects(),
                Instant.now().toString());

        // No MongoDB write here. Downstream consumers update their own databases.
        var message = MessageBuilder.withBody(json.writeValueAsBytes(event))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbit.send("game.events", "choice.made", message);
        return ResponseEntity.accepted().body(new ChoiceReceipt(eventId, choice.nextSceneId()));
    }

    private SceneDocument requireScene(String id) {
        return scenes.findById(id)
                .filter(scene -> !scene.deleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scene not found"));
    }

    private static SceneView toView(SceneDocument scene) {
        return new SceneView(scene.id(), scene.version(), scene.title(), scene.text(), scene.choices());
    }

    public record SceneView(String id, long version, String title, String text, List<Choice> choices) {}
    public record NextScene(String id, String title) {}
    public record ChooseRequest(String characterId) {}
    public record ChoiceReceipt(String eventId, String nextSceneId) {}
}
