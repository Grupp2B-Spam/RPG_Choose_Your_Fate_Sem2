package dk.ek.gruppe2.storycontent.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Publishes events onto this service's own exchange. The body is serialised here rather
 * than left to a message converter, so the AMQP properties and the JSON body are set in one
 * place. They carry the same values, which makes the broker UI readable without decoding
 * the body.
 */
@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> void publish(String routingKey, T payload) {
        EventEnvelope<T> envelope = EventEnvelope.of(routingKey, payload);
        byte[] body = objectMapper.writeValueAsBytes(envelope);

        Message message = MessageBuilder.withBody(body)
                .setContentType("application/json")
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(envelope.eventId())
                .setType(envelope.eventType())
                .setTimestamp(Date.from(envelope.occurredAt()))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        rabbitTemplate.send(RabbitConfig.EXCHANGE, routingKey, message);

        log.info("published {} eventId={} to exchange={}",
                routingKey, envelope.eventId(), RabbitConfig.EXCHANGE);
    }
}
