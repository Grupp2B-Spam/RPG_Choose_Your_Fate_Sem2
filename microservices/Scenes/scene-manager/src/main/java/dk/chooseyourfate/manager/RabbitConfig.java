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
    TopicExchange sceneManagerEvents() {
        return new TopicExchange("scenemanager.events", true, false);
    }

    // DEVELOPMENT-ONLY stub queues so that the sample can demonstrate fan-out
    // without implementing StoryPathManager and CharacterManager. In a real
    // deployment those services should provision their own subscriptions.
    @Bean
    Queue demoStoryPathQueue() {
        return QueueBuilder.durable("storypathmanager.player-choice").build();
    }

    @Bean
    Queue demoCharacterQueue() {
        return QueueBuilder.durable("charactermanager.player-choice").build();
    }

    @Bean
    Binding demoStoryPathBinding(@Qualifier("demoStoryPathQueue") Queue demoStoryPathQueue,
                                 TopicExchange sceneManagerEvents) {
        return BindingBuilder.bind(demoStoryPathQueue).to(sceneManagerEvents).with("player-choice.made");
    }

    @Bean
    Binding demoCharacterBinding(@Qualifier("demoCharacterQueue") Queue demoCharacterQueue,
                                 TopicExchange sceneManagerEvents) {
        return BindingBuilder.bind(demoCharacterQueue).to(sceneManagerEvents).with("player-choice.made");
    }
}
