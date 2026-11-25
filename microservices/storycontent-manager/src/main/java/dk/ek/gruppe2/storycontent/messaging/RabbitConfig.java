package dk.ek.gruppe2.storycontent.messaging;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The broker topology this service creates: its exchange, the consumer queues bound to
 * it, and the dead-letter path.
 *
 * <p><b>Declaring the consumers' queues is temporary.</b> Normally a consumer declares its
 * own queue, so that adding one needs no change here. We do it instead because an exchange
 * stores nothing: it routes to the queues bound right now and drops the message if there
 * are none — and the publish call still succeeds. A consumer that has never started yet
 * has no queue, so everything published before its first run is lost silently. Seven
 * services are being built in parallel, so nobody can guarantee who starts first.
 *
 * <p>The cost is that this service has to know its consumers. A fourth consumer means
 * editing this file, which is the coupling we split the system up to avoid. The way out,
 * once there is a cluster, is to create the topology at deploy time instead.
 */
@Configuration
public class RabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitConfig.class);

    /** One topic exchange per publishing service, named {@code <service>.events}. */
    public static final String EXCHANGE = "storycontent.events";

    public static final String ROUTING_SCENE_CONTENT_UPDATED = "scene-content.updated";
    public static final String ROUTING_ITEM_DEFINITION_UPDATED = "item-definition.updated";
    public static final String ROUTING_RACE_DEFINITION_UPDATED = "race-definition.updated";

    /**
     * Consumer queues, named {@code <consumer-service>.<purpose>}.
     *
     * <p>Renaming one breaks someone else's service, because they bind to the queue we
     * created. The AsyncAPI contract lists them for that reason.
     */
    public static final String QUEUE_SCENE_CONTENT = "scenemanager.scene-content";
    public static final String QUEUE_ITEM_RULES = "charactermanager.item-rules";
    public static final String QUEUE_RACE_DEFINITIONS = "charactermanager.race-definitions";

    /** Where a message goes when a consumer rejects it instead of acknowledging it. */
    public static final String DEAD_LETTER_EXCHANGE = "dlx";

    @Bean
    public TopicExchange storyContentEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * The dead-letter exchange.
     *
     * <p>Direct, because each queue dead-letters to its own exact routing key
     * {@code <queue-name>.dlq}.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * The three consumer queues with their bindings, and a dead-letter queue for each.
     *
     * <p>Grouped in one bean so the whole topology reads in one place, and so adding a
     * consumer is one entry rather than three beans someone can half-write.
     *
     * <p>The dead-letter <em>queues</em> matter as much as the exchange. RabbitMQ does not
     * check {@code x-dead-letter-exchange} when a queue is declared, and an exchange with
     * nothing bound to it throws away what it receives — so without these, a rejected
     * message would vanish down a path that looks configured. Nothing consumes them; they
     * exist so a failed message is somewhere a human can find it.
     */
    @Bean
    public Declarables consumerTopology(
            TopicExchange storyContentEventsExchange,
            DirectExchange deadLetterExchange) {

        Queue sceneContent = consumerQueue(QUEUE_SCENE_CONTENT);
        Queue itemRules = consumerQueue(QUEUE_ITEM_RULES);
        Queue raceDefinitions = consumerQueue(QUEUE_RACE_DEFINITIONS);

        return new Declarables(List.of(
                sceneContent,
                itemRules,
                raceDefinitions,

                BindingBuilder.bind(sceneContent).to(storyContentEventsExchange)
                        .with(ROUTING_SCENE_CONTENT_UPDATED),
                BindingBuilder.bind(itemRules).to(storyContentEventsExchange)
                        .with(ROUTING_ITEM_DEFINITION_UPDATED),
                BindingBuilder.bind(raceDefinitions).to(storyContentEventsExchange)
                        .with(ROUTING_RACE_DEFINITION_UPDATED),

                deadLetterQueue(QUEUE_SCENE_CONTENT),
                deadLetterQueue(QUEUE_ITEM_RULES),
                deadLetterQueue(QUEUE_RACE_DEFINITIONS),

                deadLetterBinding(QUEUE_SCENE_CONTENT, deadLetterExchange),
                deadLetterBinding(QUEUE_ITEM_RULES, deadLetterExchange),
                deadLetterBinding(QUEUE_RACE_DEFINITIONS, deadLetterExchange)));
    }

    /**
     * Durable, and deliberately neither auto-delete nor exclusive.
     *
     * <p>Auto-delete would drop the queue when the last consumer disconnects — which is
     * what an autoscaler causes on scale-down, reopening the very gap these queues close.
     * Exclusive would stop a second instance consuming at all, so the consumer could never
     * be scaled. {@code QueueBuilder.durable} gives durable and excludes both.
     *
     * <p>Arguments are part of a queue's identity: a consumer that redeclares one of these
     * with different arguments gets {@code PRECONDITION_FAILED} and will not start.
     */
    private Queue consumerQueue(String name) {
        return QueueBuilder.durable(name)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", DEAD_LETTER_EXCHANGE,
                        "x-dead-letter-routing-key", deadLetterName(name)))
                .build();
    }

    private Queue deadLetterQueue(String consumerQueueName) {
        return QueueBuilder.durable(deadLetterName(consumerQueueName)).build();
    }

    private Binding deadLetterBinding(String consumerQueueName, DirectExchange dlx) {
        return BindingBuilder.bind(deadLetterQueue(consumerQueueName))
                .to(dlx)
                .with(deadLetterName(consumerQueueName));
    }

    private String deadLetterName(String consumerQueueName) {
        return consumerQueueName + ".dlq";
    }

    /**
     * Publishes with {@code mandatory} set, so the broker hands back a message it could
     * not route instead of dropping it.
     *
     * <p>Catches a deleted binding, a routing-key typo, or a queue renamed here without
     * its binding. The publish call succeeds in all of those, so without this they look
     * exactly like a working system.
     *
     * <p>The callback is asynchronous — {@code publish()} has already returned — and it
     * reports a routing failure, not a delivery failure.
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
