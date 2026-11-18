package dk.ek.gruppe2.storycontent.messaging;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the exchange this service owns.
 *
 * <p>Per the contract in {@code microservices/contracts/README.md}, a publisher owns its
 * exchange and nothing else. Consumer queues and bindings are declared by the consumers,
 * so adding a listener to one of our events requires no change here.
 */
@Configuration
public class RabbitConfig {

    /** One topic exchange per publishing service, named {@code <service>.events}. */
    public static final String EXCHANGE = "storycontent.events";

    public static final String ROUTING_SCENE_CONTENT_UPDATED = "scene-content.updated";
    public static final String ROUTING_ITEM_DEFINITION_UPDATED = "item-definition.updated";

    @Bean
    public TopicExchange storyContentEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }
}
