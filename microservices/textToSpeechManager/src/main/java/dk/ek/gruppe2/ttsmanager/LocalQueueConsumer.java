package dk.ek.gruppe2.ttsmanager;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Development convenience only. Knative deployments use authenticated CloudEvent delivery instead. */
@Component
@Profile("local")
public class LocalQueueConsumer {
    private final SpeechEvents events;
    private final ObjectMapper json;
    public LocalQueueConsumer(SpeechEvents events, ObjectMapper json) { this.events = events; this.json = json; }

    @RabbitListener(queues = "${tts.queue}")
    public void receive(byte[] message) {
        try { events.process(json.readTree(message)); }
        catch (IllegalArgumentException | tools.jackson.core.JacksonException invalid) {
            throw new AmqpRejectAndDontRequeueException("Invalid speech event", invalid);
        }
    }
}
