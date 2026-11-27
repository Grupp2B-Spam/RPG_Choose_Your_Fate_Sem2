package dk.ek.gruppe2.ttsmanager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/** A cache miss is acknowledged to the RPC caller only after the broker confirms durable work. */
public final class RabbitWorkPublisher implements WorkPublisher {
    private final RabbitTemplate rabbit;
    private final ObjectMapper json;
    private final String queue;
    private final long timeoutMillis;

    public RabbitWorkPublisher(RabbitTemplate rabbit, ObjectMapper json, String queue, long timeoutMillis) {
        this.rabbit = rabbit; this.json = json; this.queue = queue; this.timeoutMillis = timeoutMillis;
    }

    @Override public void publish(SpeechSpec spec) {
        String id = UUID.randomUUID().toString();
        var metadata = new MessageProperties();
        metadata.setMessageId(id);
        metadata.setType("SpeechRequested");
        metadata.setContentType("application/json");
        metadata.setContentEncoding("UTF-8");
        metadata.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        // Synthesis settings were snapshotted in our metadata vault before publication.
        byte[] payload = json.writeValueAsBytes(Map.of("eventId", id, "eventType", "SpeechRequested", "eventVersion", 2,
                "contentHash", spec.contentHash()));
        var confirmation = new CorrelationData(id);
        try {
            // The service owns this queue; primary RecapGenerated events reach it through its binding.
            rabbit.send("", queue, new Message(payload, metadata), confirmation);
            if (!confirmation.getFuture().get(timeoutMillis, TimeUnit.MILLISECONDS).ack() || confirmation.getReturned() != null) {
                throw new IllegalStateException("Speech work was not routed and confirmed");
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Speech work publication interrupted", error);
        } catch (Exception error) { throw new IllegalStateException("Speech work publication failed", error); }
    }
}
