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
    TopicExchange storyContentEvents() {
        return new TopicExchange("storycontent.events", true, false);
    }

    @Bean
    Queue sceneChanges() {
        return QueueBuilder.durable("scene-indexer.scene-changed")
                .deadLetterExchange("dlx")
                .deadLetterRoutingKey("scene-indexer.scene-changed.dlq")
                .build();
    }

    @Bean
    Binding sceneChangesBinding(@Qualifier("sceneChanges") Queue sceneChanges,
                                @Qualifier("storyContentEvents") TopicExchange storyContentEvents) {
        return BindingBuilder.bind(sceneChanges).to(storyContentEvents).with("scene-content.updated");
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx", true, false);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable("scene-indexer.scene-changed.dlq").build();
    }

    @Bean
    Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange)
                .with("scene-indexer.scene-changed.dlq");
    }
}
