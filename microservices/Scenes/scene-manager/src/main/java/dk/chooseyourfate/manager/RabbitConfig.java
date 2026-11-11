package dk.chooseyourfate.manager;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
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

    // DEVELOPMENT-ONLY stub queues so that the sample can demonstrate fan-out
    // without implementing StoryPathManager and CharacterManager. In a real
    // deployment those services should provision their own subscriptions.
    @Bean
    Queue demoStoryPathQueue() {
        return QueueBuilder.durable("demo.storypath.choice-made").build();
    }

    @Bean
    Queue demoCharacterQueue() {
        return QueueBuilder.durable("demo.character.choice-made").build();
    }

    @Bean
    Binding demoStoryPathBinding(@Qualifier("demoStoryPathQueue") Queue demoStoryPathQueue, TopicExchange gameEvents) {
        return BindingBuilder.bind(demoStoryPathQueue).to(gameEvents).with("choice.made");
    }

    @Bean
    Binding demoCharacterBinding(@Qualifier("demoCharacterQueue") Queue demoCharacterQueue, TopicExchange gameEvents) {
        return BindingBuilder.bind(demoCharacterQueue).to(gameEvents).with("choice.made");
    }
}
