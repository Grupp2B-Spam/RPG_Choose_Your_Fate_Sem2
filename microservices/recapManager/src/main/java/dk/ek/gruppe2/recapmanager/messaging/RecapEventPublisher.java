package dk.ek.gruppe2.recapmanager.messaging;

import dk.ek.gruppe2.recapmanager.config.RecapMessagingProperties;
import dk.ek.gruppe2.recapmanager.model.RecapOutboxMessage;
import dk.ek.gruppe2.recapmanager.repository.RecapOutboxRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecapEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(RecapEventPublisher.class);
    private final RecapOutboxRepository repository;
    private final RabbitTemplate rabbit;
    private final RecapMessagingProperties properties;

    public RecapEventPublisher(RecapOutboxRepository repository, RabbitTemplate rabbit,
                               RecapMessagingProperties properties) {
        this.repository = repository;
        this.rabbit = rabbit;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.recap-events.poll-delay-millis:2000}")
    public void publishPending() {
        for (var event : repository.findTop25ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant.now())) {
            publish(event);
        }
    }

    void publish(RecapOutboxMessage event) {
        try {
            var metadata = new MessageProperties();
            metadata.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            metadata.setContentEncoding("UTF-8");
            metadata.setMessageId(event.getId());
            metadata.setType("RecapGenerated");
            metadata.setHeader("eventVersion", 1);
            metadata.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            var correlation = new CorrelationData(event.getId());
            rabbit.send(properties.getExchange(), properties.getRoutingKey(),
                    new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), metadata), correlation);
            var confirmation = correlation.getFuture().get(properties.getConfirmTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (!confirmation.ack() || correlation.getReturned() != null) {
                throw new IllegalStateException("Broker rejected or returned the message");
            }
            // Crash after broker ack but before this delete can duplicate delivery; eventId remains stable.
            repository.delete(event);
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            event.retryAt(Instant.now().plusMillis(properties.getRetryDelayMillis()));
            repository.save(event);
            log.warn("Recap event {} not confirmed; retained for retry ({})", event.getId(),
                    exception.getClass().getSimpleName());
        }
    }
}
