package dk.chooseyourfate.indexer;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange gameEvents() {
        return new TopicExchange("game.events", true, false);
    }

    @Bean
    Queue sceneChanges() {
        return QueueBuilder.durable("scene-indexer.scene-changed")
                .deadLetterExchange("game.events.dlx")
                .deadLetterRoutingKey("scene.changed.failed")
                .build();
    }

    @Bean
    Binding sceneChangesBinding(@Qualifier("sceneChanges") Queue sceneChanges, TopicExchange gameEvents) {
        return BindingBuilder.bind(sceneChanges).to(gameEvents).with("scene.changed");
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange("game.events.dlx", true, false);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable("scene-indexer.dead").build();
    }

    @Bean
    Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("scene.changed.failed");
    }
}
