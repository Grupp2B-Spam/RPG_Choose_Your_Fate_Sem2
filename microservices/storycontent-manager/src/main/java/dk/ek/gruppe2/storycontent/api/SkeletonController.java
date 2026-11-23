package dk.ek.gruppe2.storycontent.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.ek.gruppe2.storycontent.messaging.EventPublisher;
import dk.ek.gruppe2.storycontent.messaging.RabbitConfig;

/**
 * Temporary endpoints for the walking skeleton.
 *
 * <p>They exist only to prove that the service boots, reaches RabbitMQ and publishes a
 * message shaped like the real contract. Delete this class once the real admin API
 * exists; nothing should be built on top of it.
 */
@RestController
@RequestMapping("/admin/v1/test-events")
public class SkeletonController {

    private final EventPublisher publisher;

    public SkeletonController(EventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/scene-content")
    public ResponseEntity<Map<String, String>> publishSceneContent() {
        Map<String, Object> payload = Map.of(
                "sceneId", 42,
                "sceneVersion", 1,
                "status", "PUBLISHED",
                "chapterId", 3,
                "name", "The innkeeper",
                "description", "A low fire burns in the hearth. The innkeeper watches you.",
                "choices", List.of(Map.of(
                        "choiceId", 118,
                        "label", "Trust him",
                        "beat", "You chose to trust the innkeeper.",
                        "destinationSceneId", 58,
                        "weight", 2,
                        "grantedItemIds", List.of())),
                "npcIds", List.of(12),
                "questIds", List.of(5));

        publisher.publish(RabbitConfig.ROUTING_SCENE_CONTENT_UPDATED, payload);
        return ResponseEntity.accepted().body(Map.of("published", RabbitConfig.ROUTING_SCENE_CONTENT_UPDATED));
    }

    @PostMapping("/item-definition")
    public ResponseEntity<Map<String, String>> publishItemDefinition() {
        Map<String, Object> payload = Map.of(
                "itemId", 31,
                "itemVersion", 1,
                "status", "ACTIVE",
                "type", "EQUIPMENT",
                "slot", "MAINHAND",
                "requirements", Map.of(),
                "modifiers", Map.of("attack", 3));

        publisher.publish(RabbitConfig.ROUTING_ITEM_DEFINITION_UPDATED, payload);
        return ResponseEntity.accepted().body(Map.of("published", RabbitConfig.ROUTING_ITEM_DEFINITION_UPDATED));
    }
}
