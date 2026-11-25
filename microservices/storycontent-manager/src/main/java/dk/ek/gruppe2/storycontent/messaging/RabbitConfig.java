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
 * Declares this service's exchange, and — for now — the consumer queues bound to it.
 *
 * <p><b>This is an interim arrangement that amends ADR-004.</b> The rule in
 * {@code microservices/contracts/README.md} was that a publisher owns its exchange and a
 * consumer owns its queue, which is what keeps the two independently deployable. ADR-021
 * suspends the second half of that rule, and ADR-004 remains the state we intend to
 * return to. It is written here rather than left in the decision log because the next
 * person to read this class will otherwise see it contradicting the convention document
 * and assume one of the two is simply wrong.
 *
 * <p><b>Why.</b> An exchange stores nothing. A topic exchange routes to whatever queues
 * are bound at the moment of publication and discards the message if there are none — and
 * the publish call still succeeds. Until a consumer has started for the first time its
 * queue does not exist, so every event published in that window is lost with no error on
 * either side. With seven services being built in parallel by different people, nobody can
 * rely on deployment ordering to close that window. Declaring the queues here closes it
 * from the first start.
 *
 * <p><b>What it costs.</b> This service now has to know who its consumers are, which is
 * exactly the knowledge the decomposition set out to remove. Adding a fourth consumer
 * becomes a change in this repository, made by someone who does not own that consumer.
 * That is the trade ADR-021 accepted deliberately; it is not free and should not be
 * described as though it were.
 *
 * <p><b>What consumers still own:</b> prefetch, concurrency, acknowledgement mode, and
 * what they do with a dead-lettered message. Only the declaration moved. Note that queue
 * <em>arguments</em> are part of the declaration, so the dead-letter settings below are
 * fixed by this service — a consumer that redeclares one of these queues with different
 * arguments gets {@code PRECONDITION_FAILED} and will not start.
 *
 * <p>The longer-term answer, for when the cluster exists, is topology as code applied at
 * deploy time: each queue definition stays in its consumer's repository but is applied
 * independently of whether that consumer has ever run. That has the reliability property
 * this arrangement has and the ownership property ADR-004 wants.
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
     */
    public static final String ROUTING_RACE_DEFINITION_UPDATED = "race-definition.updated";

    /**
     * Consumer queues, named {@code <consumer-service>.<purpose>} so that the broker UI
     * makes it obvious who owns what.
     *
     * <p>These names are now a cross-team commitment rather than a suggestion: a consumer
     * binds to the queue this service declared, so changing one of these strings breaks
     * someone else's service. They are listed in the AsyncAPI contract for that reason.
     */
    public static final String QUEUE_SCENE_CONTENT = "scenemanager.scene-content";
    public static final String QUEUE_ITEM_RULES = "charactermanager.item-rules";
    public static final String QUEUE_RACE_DEFINITIONS = "charactermanager.race-definitions";

    /** Where a message goes when a consumer rejects it rather than acknowledging it. */
    public static final String DEAD_LETTER_EXCHANGE = "dlx";

    @Bean
    public TopicExchange storyContentEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * The dead-letter exchange, and a dead-letter queue per consumer queue.
     *
     * <p>The convention document requires every consumer queue to set
     * {@code x-dead-letter-exchange: dlx}, and nothing had ever declared {@code dlx}.
     * RabbitMQ does not validate that argument when the queue is declared, so this did not
     * fail at startup — it failed silently later: a rejected message is routed to an
     * exchange that does not exist and is dropped, which is the outcome dead-lettering is
     * supposed to prevent.
     *
     * <p>Declaring the exchange alone would not have been enough either. An exchange with
     * no queue bound to it discards what it receives, exactly like the first-start window
     * above — so the dead-letter path would still have looked configured and still lost
     * every message. The queues are what make it real.
     *
     * <p>It is a direct exchange because each queue dead-letters to its own routing key,
     * {@code <queue-name>.dlq}, and an exact match is what is wanted. Nothing consumes
     * these queues; they exist so a failed message is somewhere a human can look at it.
     * Retry counts and backoff are still undecided — see {@code OPEN-DESIGN-ISSUES.md} 3.2
     * — but the destination can exist before the policy does.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * The three consumer queues, their bindings, and their dead-letter queues.
     *
     * <p>Grouped in one {@link Declarables} rather than spread over a dozen beans so that
     * the topology can be read in one place — and so that adding a consumer is one entry
     * rather than three beans someone can half-write.
     *
     * <p>Three properties are not a matter of taste, and getting them wrong looks correct
     * in development:
     *
     * <ul>
     *   <li>{@code durable} — or the queue disappears on a broker restart, taking the
     *       backlog with it.</li>
     *   <li>not {@code auto-delete} — or the queue is deleted when the last consumer
     *       disconnects, which is exactly what an autoscaler does on scale-down. That
     *       would reopen the loss window this whole arrangement exists to close.</li>
     *   <li>not {@code exclusive} — or only the declaring connection may consume, so a
     *       second instance cannot start at all and the consumer cannot be scaled.</li>
     * </ul>
     *
     * <p>{@link QueueBuilder#durable} gives the first and excludes the other two.
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
     * Publishes with {@code mandatory} set, so the broker hands a message back instead of
     * discarding it when no queue is bound to its routing key.
     *
     * <p>Still worth keeping now that this service declares the queues itself. It was the
     * only detection for the first-start window, and that window is closed — but the
     * callback also catches a binding deleted by hand in the broker UI, a routing key
     * typo, and the case where someone changes a queue name here without changing the
     * binding. The publish call succeeds in all of those, so without this they are
     * indistinguishable from a working system.
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
