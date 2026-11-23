package dk.ek.gruppe2.storycontent.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the exchange this service owns, and the template used to publish to it.
 *
 * <p>Per the contract in {@code microservices/contracts/README.md}, a publisher owns its
 * exchange and nothing else. Consumer queues and bindings are declared by the consumers,
 * so adding a listener to one of our events requires no change here.
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    /** One topic exchange per publishing service, named {@code <service>.events}. */
    public static final String EXCHANGE = "storycontent.events";

    public static final String ROUTING_SCENE_CONTENT_UPDATED = "scene-content.updated";
    public static final String ROUTING_ITEM_DEFINITION_UPDATED = "item-definition.updated";

    /**
     * Added in version 1.1.0 of the contract. Races were resolved to this service rather
     * than to CharacterManager, which still needs a race's starting chapter at character
     * creation — so it gets one as an event to cache rather than a call to make. See
     * ADR-017.
     *
     * <p>Nothing else changes here: a topic exchange routes a new key without being
     * redeclared, and the consumer declares the queue and the binding (ADR-004). Adding a
     * third event is therefore a change to this service and to the contract, and to
     * nothing else.
     */
    public static final String ROUTING_RACE_DEFINITION_UPDATED = "race-definition.updated";

    @Bean
    public TopicExchange storyContentEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Publishes with {@code mandatory} set, so the broker hands a message back instead of
     * discarding it when no queue is bound to its routing key.
     *
     * <p>An exchange stores nothing. Without this, a message published to a routing key
     * nobody listens to disappears with no error anywhere — the publish call still
     * succeeds. That is indistinguishable from a working system until someone notices the
     * data never arrived, which is why it is worth catching here.
     *
     * <p>The return is asynchronous: {@code publish()} has already returned by the time
     * the callback fires. It reports a routing failure, not a delivery failure.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        template.setReturnsCallback(returned ->
                log.error("Unroutable message: no queue is bound to '{}' on exchange '{}'. "
                                + "messageId={} replyCode={} replyText={}",
                        returned.getRoutingKey(),
                        returned.getExchange(),
                        returned.getMessage().getMessageProperties().getMessageId(),
                        returned.getReplyCode(),
                        returned.getReplyText()));
        return template;
    }
}
