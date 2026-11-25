package dk.chooseyourfate.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class SceneControllerTest {
    @Test
    void acceptedChoicePublishesSnapshotForBothSubscribers() {
        SceneRepository repository = mock(SceneRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        JsonMapper json = JsonMapper.builder().build();
        when(repository.findById("start")).thenReturn(Optional.of(new SceneDocument(
                "start", 3, "Start", "A fork", List.of(new Choice(
                        "left", "Go left", "mill", Map.of("courage", 1))), false)));
        SceneController controller = new SceneController(repository, rabbit, json);

        var response = controller.choose("start", "left", new SceneController.ChooseRequest("char-1"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("mill", response.getBody().nextSceneId());
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(rabbit).send(eq("game.events"), eq("choice.made"), messages.capture());
        ChoiceMadeEvent published = json.readValue(messages.getValue().getBody(), ChoiceMadeEvent.class);
        assertEquals("char-1", published.characterId());
        assertEquals(3L, published.sceneVersion());
        assertEquals("Go left", published.choiceText());
        assertEquals(1, published.effects().get("courage"));
    }

    @Test
    void rejectsChoiceNotInSceneWithoutPublishing() {
        SceneRepository repository = mock(SceneRepository.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        when(repository.findById("start")).thenReturn(Optional.of(new SceneDocument(
                "start", 1, "Start", "A fork", List.of(new Choice("left", "Go left", "mill", Map.of())), false)));
        SceneController controller = new SceneController(repository, rabbit, JsonMapper.builder().build());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.choose("start", "unknown", new SceneController.ChooseRequest("char-1")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(rabbit);
    }
}
