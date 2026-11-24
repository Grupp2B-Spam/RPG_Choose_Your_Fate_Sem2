package dk.ek.gruppe2.recapmanager.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RecapMessagingProperties.class)
public class RecapMessagingConfig {
    @Bean
    public TopicExchange recapExchange(RecapMessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public RabbitTemplate recapRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // An unbound exchange must not count as a successful handoff to TTS.
        template.setMandatory(true);
        return template;
    }
}
