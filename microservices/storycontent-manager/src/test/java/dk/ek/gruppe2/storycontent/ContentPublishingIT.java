package dk.ek.gruppe2.storycontent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ChapterResponse;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemResponse;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcResponse;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestResponse;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.RaceResponse;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.ChoiceRequest;
import dk.ek.gruppe2.storycontent.api.ApiExceptions.InvalidContentException;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.EffectRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneResponse;
import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.EffectKind;
import dk.ek.gruppe2.storycontent.domain.Enums.EquipmentSlot;
import dk.ek.gruppe2.storycontent.domain.Enums.ItemType;
import dk.ek.gruppe2.storycontent.messaging.RabbitConfig;
import dk.ek.gruppe2.storycontent.service.ChapterService;
import dk.ek.gruppe2.storycontent.service.ItemService;
import dk.ek.gruppe2.storycontent.service.NpcService;
import dk.ek.gruppe2.storycontent.service.QuestService;
import dk.ek.gruppe2.storycontent.service.RaceService;
import dk.ek.gruppe2.storycontent.service.SceneService;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end: a real MySQL built from this service's own {@code db-init}, a real RabbitMQ,
 * and a test queue bound the way a consumer would bind one.
 *
 * <p>What is being tested is not "does it publish" — the skeleton established that against
 * a real broker before any of this existed. It is the discipline around
 * {@code sceneVersion}, {@code itemVersion} and {@code raceVersion}, because those are
 * what the consumers' correctness rests on (ADR-006) and what makes them safe to scale
 * (ADR-011). A version that fails to increment does not merely lose an update: it produces
 * a message that looks older than what the consumer already holds, so the consumer
 * discards it and the read model stays wrong with nothing logged anywhere.
 *
 * <p>The test declares its own queue and binding rather than the service declaring one.
 * That is the ownership rule from {@code contracts/README.md} being exercised rather than
 * described: a publisher owns its exchange and knows nothing about who listens, so adding
 * a consumer — including this one — requires no change on the publishing side.
 */
@SpringBootTest
@Import({StoryContentContainers.class, ContentPublishingIT.TestQueues.class})
@DisplayName("Content changes are published with an honest version")
class ContentPublishingIT {

    /**
     * Makes each fixture's race name unique. Race names are unique system-wide, and this
     * class's writes really commit, so reusing one name would make every test after the
     * first fail on the second fixture.
     */
    private static final AtomicInteger FIXTURE_COUNT = new AtomicInteger();

    private static final String SCENE_QUEUE = "it.scene-content";
    private static final String ITEM_QUEUE = "it.item-definition";
    private static final String RACE_QUEUE = "it.race-definition";

    /**
     * Queues declared by the consumer, as the conventions require: durable, not
     * auto-delete, not exclusive. Those three are not a matter of taste — an auto-delete
     * queue disappears when the last consumer disconnects, which is exactly what an
     * autoscaler causes on scale-down.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class TestQueues {

        @Bean
        Queue sceneQueue() {
            return QueueBuilder.durable(SCENE_QUEUE).build();
        }

        @Bean
        Queue itemQueue() {
            return QueueBuilder.durable(ITEM_QUEUE).build();
        }

        @Bean
        Queue raceQueue() {
            return QueueBuilder.durable(RACE_QUEUE).build();
        }

        @Bean
        Binding sceneBinding(Queue sceneQueue, TopicExchange storyContentEventsExchange) {
            return BindingBuilder.bind(sceneQueue).to(storyContentEventsExchange)
                    .with(RabbitConfig.ROUTING_SCENE_CONTENT_UPDATED);
        }

        @Bean
        Binding itemBinding(Queue itemQueue, TopicExchange storyContentEventsExchange) {
            return BindingBuilder.bind(itemQueue).to(storyContentEventsExchange)
                    .with(RabbitConfig.ROUTING_ITEM_DEFINITION_UPDATED);
        }

        @Bean
        Binding raceBinding(Queue raceQueue, TopicExchange storyContentEventsExchange) {
            return BindingBuilder.bind(raceQueue).to(storyContentEventsExchange)
                    .with(RabbitConfig.ROUTING_RACE_DEFINITION_UPDATED);
        }
    }

    @Autowired private ChapterService chapters;
    @Autowired private SceneService scenes;
    @Autowired private ItemService items;
    @Autowired private RaceService races;
    @Autowired private NpcService npcs;
    @Autowired private QuestService quests;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private RabbitAdmin rabbitAdmin;
    @Autowired private ObjectMapper json;

    @BeforeEach
    void drainQueues() {
        drain(SCENE_QUEUE);
        drain(ITEM_QUEUE);
        drain(RACE_QUEUE);
    }

    @Test
    @DisplayName("the consumer queues and the dead-letter path exist before any consumer has run")
    void thePublisherDeclaresTheConsumerTopology() {
        // The whole point of ADR-021. No consumer has ever started against this broker —
        // these containers were created seconds ago — so if the queues are here, this
        // service put them here. Without them an event published now would be routed to
        // nothing and discarded, with the publish call still succeeding.
        //
        // The other tests in this class cannot show this: they declare their own queues,
        // so they would pass just as happily against the old arrangement.
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_SCENE_CONTENT)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_ITEM_RULES)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_RACE_DEFINITIONS)).isNotNull();

        // Declaring dlx alone would not have been enough: an exchange with nothing bound
        // to it discards what it receives, so the dead-letter path would still have looked
        // configured and still lost every message.
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_SCENE_CONTENT + ".dlq")).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_ITEM_RULES + ".dlq")).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitConfig.QUEUE_RACE_DEFINITIONS + ".dlq")).isNotNull();
    }

    @Test
    @DisplayName("a real consumer queue receives the event, not only the test's own queue")
    void theDeclaredConsumerQueueIsBoundToTheRoutingKey() {
        drain(RabbitConfig.QUEUE_SCENE_CONTENT);

        ChapterResponse chapter = chapters.create(new ChapterRequest("Moonlit Ruins"));
        SceneResponse scene = scenes.create(new SceneRequest(
                chapter.id(), "The innkeeper", "A low fire burns.", null));

        // Asserting on the queue SceneManager will actually consume from, rather than on
        // the one this test declared. A queue that exists but is bound to the wrong
        // routing key is silent in exactly the same way as no queue at all.
        Message message = poll(RabbitConfig.QUEUE_SCENE_CONTENT, Duration.ofSeconds(10));
        assertThat(message).as("scenemanager.scene-content should have received the event").isNotNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) readMap(message).get("payload");
        assertThat(payload.get("sceneId")).isEqualTo(scene.id());
    }

    @Test
    @DisplayName("a new scene is published at version 1, with the envelope the contract describes")
    void aNewSceneIsPublishedAtVersionOne() {
        ChapterResponse chapter = chapters.create(new ChapterRequest("Moonlit Ruins"));
        SceneResponse scene = scenes.create(new SceneRequest(
                chapter.id(), "The innkeeper", "A low fire burns in the hearth.", null));

        Map<String, Object> envelope = nextMessage(SCENE_QUEUE);

        assertThat(envelope).containsKeys("eventId", "eventType", "occurredAt", "schemaVersion", "payload");
        assertThat(envelope.get("eventType")).isEqualTo(RabbitConfig.ROUTING_SCENE_CONTENT_UPDATED);
        assertThat(envelope.get("schemaVersion")).isEqualTo(1);

        Map<String, Object> payload = payloadOf(envelope);
        assertThat(payload.get("sceneId")).isEqualTo(scene.id());
        assertThat(payload.get("sceneVersion")).isEqualTo(1);
        assertThat(payload.get("status")).isEqualTo("PUBLISHED");
        assertThat(payload.get("description")).isEqualTo("A low fire burns in the hearth.");
    }

    @Test
    @DisplayName("adding a choice bumps the scene's version, because a consumer can see it")
    void addingAChoiceBumpsTheSceneVersion() {
        Fixture fixture = fixture();
        drain(SCENE_QUEUE);

        scenes.addChoice(fixture.sceneId, new ChoiceRequest(
                fixture.destinationSceneId,
                "Trust him",
                "You chose to trust the innkeeper.",
                (short) 2,
                0,
                List.of(),
                List.of(new EffectRequest(EffectKind.GRANT_ITEM, fixture.itemId, null, null, null, null))));

        Map<String, Object> payload = payloadOf(nextMessage(SCENE_QUEUE));
        assertThat(payload.get("sceneVersion")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) payload.get("choices");
        assertThat(choices).hasSize(1);
        assertThat(choices.get(0).get("beat")).isEqualTo("You chose to trust the innkeeper.");
        assertThat(choices.get(0).get("grantedItemIds")).isEqualTo(List.of(fixture.itemId));
    }

    @Test
    @DisplayName("the version keeps climbing across successive edits")
    void theVersionIsMonotonic() {
        Fixture fixture = fixture();
        drain(SCENE_QUEUE);

        for (int expected = 2; expected <= 5; expected++) {
            scenes.update(fixture.sceneId, new SceneRequest(
                    fixture.chapterId, "Edit " + expected, null, null));
            assertThat(payloadOf(nextMessage(SCENE_QUEUE)).get("sceneVersion"))
                    .as("every observable change must produce a strictly newer version, or "
                            + "a consumer discards it as stale")
                    .isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("renaming an NPC publishes nothing: no consumer can observe it")
    void renamingAnNpcPublishesNothing() {
        Fixture fixture = fixture();
        NpcResponse npc = npcs.create(new NpcRequest("Captain Elira", fixture.raceId));
        scenes.replaceNpcs(fixture.sceneId, List.of(npc.id()));
        nextMessage(SCENE_QUEUE);

        npcs.update(npc.id(), new NpcRequest("Captain Elira the Bold", fixture.raceId));

        // Only npcIds travels in the scene payload, so a rename changes nothing on the
        // wire. Republishing it would inflate the number consumers compare against for no
        // information at all — ADR-006 says "every change consumers can observe", and this
        // is the other half of that sentence.
        assertNothingPublished(SCENE_QUEUE);
    }

    @Test
    @DisplayName("deleting an NPC republishes every scene it stood in")
    void deletingAnNpcRepublishesTheScenesItStoodIn() {
        Fixture fixture = fixture();
        NpcResponse npc = npcs.create(new NpcRequest("Merchant Vonn", fixture.raceId));
        scenes.replaceNpcs(fixture.sceneId, List.of(npc.id()));
        int versionWithNpc = (int) payloadOf(nextMessage(SCENE_QUEUE)).get("sceneVersion");

        npcs.delete(npc.id());

        Map<String, Object> payload = payloadOf(nextMessage(SCENE_QUEUE));
        assertThat(payload.get("npcIds")).isEqualTo(List.of());
        assertThat((int) payload.get("sceneVersion")).isGreaterThan(versionWithNpc);
    }

    @Test
    @DisplayName("creating a quest bumps its scene's version and appears in questIds")
    void creatingAQuestRepublishesItsScene() {
        Fixture fixture = fixture();
        drain(SCENE_QUEUE);

        QuestResponse quest = quests.create(fixture.sceneId, new QuestRequest(
                "Before the doors close",
                "Find the hidden moon key before the shrine doors are sealed.",
                List.of(fixture.itemId)));

        // The response is built before the version is incremented, because incrementing
        // runs as a bulk update that clears the persistence context. Asserting on it is
        // what keeps that ordering from being quietly reversed later.
        assertThat(quest.sceneId()).isEqualTo(fixture.sceneId);
        assertThat(quest.rewardItemIds()).containsExactly(fixture.itemId);

        Map<String, Object> payload = payloadOf(nextMessage(SCENE_QUEUE));
        assertThat(payload.get("questIds")).isEqualTo(List.of(quest.id()));
        assertThat(payload.get("sceneVersion")).isEqualTo(2);
    }

    @Test
    @DisplayName("editing a quest's text publishes nothing: no consumer receives it")
    void editingAQuestPublishesNothing() {
        Fixture fixture = fixture();
        QuestResponse quest = quests.create(fixture.sceneId, new QuestRequest(
                "The archive", "Return the moon key.", List.of()));
        nextMessage(SCENE_QUEUE);

        quests.update(quest.id(), new QuestRequest(
                "The archive", "Return the moon key to Archivist Nyx.", List.of()));

        // Consistent with the rule, and also a gap: CharacterManager needs a quest's
        // reward items in order to grant them, and no event carries them. Recorded as an
        // open issue rather than patched here, because adding a channel is a contract
        // change and therefore a conversation.
        assertNothingPublished(SCENE_QUEUE);
    }

    @Test
    @DisplayName("retiring an item publishes RETIRED rather than removing the definition")
    void retiringAnItemPublishesRetired() {
        ItemResponse item = items.create(new ItemRequest(
                "Rusty Sword", "Old sword.", ItemType.EQUIPMENT, EquipmentSlot.MAINHAND,
                Map.of(), Map.of(CharacterStat.CHARISMA, 2)));
        drain(ITEM_QUEUE);

        items.retire(item.id());

        Map<String, Object> payload = payloadOf(nextMessage(ITEM_QUEUE));
        assertThat(payload.get("status")).isEqualTo("RETIRED");
        assertThat(payload.get("itemVersion")).isEqualTo(2);
        // A player who owns one must still be able to use it, so the definition is still
        // there in full.
        assertThat(payload.get("slot")).isEqualTo("MAINHAND");
        assertThat(payload.get("modifiers")).isEqualTo(Map.of("charisma", 2));
    }

    @Test
    @DisplayName("changing a chapter's starting scene republishes the races that start there")
    void changingAChaptersStartingSceneRepublishesItsRaces() {
        Fixture fixture = fixture();
        drain(RACE_QUEUE);

        chapters.setStartingScene(fixture.chapterId, fixture.destinationSceneId);

        // Nothing about the race was edited. The race payload denormalises the chapter's
        // starting scene so that CharacterManager can place a new character without a
        // second lookup, so this edit changes the correct answer for every race that
        // begins here. Missing it would put new characters in a scene the author moved
        // away from — see ADR-017.
        Map<String, Object> payload = payloadOf(nextMessage(RACE_QUEUE));
        assertThat(payload.get("raceId")).isEqualTo(fixture.raceId);
        assertThat(payload.get("startingSceneId")).isEqualTo(fixture.destinationSceneId);
        assertThat(payload.get("raceVersion")).isEqualTo(2);
    }

    @Test
    @DisplayName("an effect whose fields do not match its kind is refused before it can be published")
    void anEffectThatDoesNotMatchItsKindIsRefused() {
        Fixture fixture = fixture();
        drain(SCENE_QUEUE);

        ChoiceRequest malformed = new ChoiceRequest(
                fixture.destinationSceneId, "Climb the tower", "You climbed the tower.",
                (short) 1, 0, List.of(),
                // MODIFY_STAT with no stat. The monolith's equivalent was a bare integer
                // that meant something different per consequence value, so there was
                // nothing to check it against — and its own seed data does not follow the
                // convention. ADR-016.
                List.of(new EffectRequest(EffectKind.MODIFY_STAT, null, null, null, 1, null)));

        assertThatThrownBy(() -> scenes.addChoice(fixture.sceneId, malformed))
                .isInstanceOf(InvalidContentException.class)
                .hasMessageContaining("stat");

        assertNothingPublished(SCENE_QUEUE);
    }

    // --- helpers -------------------------------------------------------------------

    private record Fixture(Integer chapterId, Integer sceneId, Integer destinationSceneId,
                           Integer itemId, Integer raceId) {
    }

    /**
     * Content for one test: a chapter with two scenes, one item, and one race starting in
     * that chapter.
     *
     * <p><b>Every test gets its own, and the database is never reset between them.</b>
     * That is not laziness — it is forced by what this class tests. Making the test
     * {@code @Transactional} would roll each one back and give clean isolation, but the
     * publication happens on an {@code AFTER_COMMIT} listener, so a transaction that is
     * rolled back never publishes anything and every assertion here would fail. The thing
     * under test is precisely what happens after the commit.
     *
     * <p>So the content accumulates across the class, and the fixture has to tolerate
     * that. Only one thing in this schema is globally unique — {@code race.name} — so only
     * the race name is made unique per call. The rest can repeat harmlessly, because every
     * assertion is scoped to ids this fixture just created.
     */
    private Fixture fixture() {
        int n = FIXTURE_COUNT.incrementAndGet();

        ChapterResponse chapter = chapters.create(new ChapterRequest("Moonlit Ruins"));
        SceneResponse scene = scenes.create(new SceneRequest(
                chapter.id(), "The innkeeper", "A low fire burns.", null));
        SceneResponse destination = scenes.create(new SceneRequest(
                chapter.id(), "Market Square", null, null));
        chapters.setStartingScene(chapter.id(), scene.id());

        ItemResponse item = items.create(new ItemRequest(
                "Moon Key", "Silver key.", ItemType.QUEST, null, Map.of(), Map.of()));
        RaceResponse race = races.create(new RaceRequest("smalls-" + n, chapter.id()));

        drainQueues();
        return new Fixture(chapter.id(), scene.id(), destination.id(), item.id(), race.id());
    }

    private Map<String, Object> nextMessage(String queue) {
        Message message = poll(queue, Duration.ofSeconds(10));
        assertThat(message).as("expected a message on %s", queue).isNotNull();
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        // The envelope values are mirrored onto the AMQP properties so that the broker UI
        // and the management tooling are useful without decoding the body.
        assertThat(message.getMessageProperties().getMessageId()).isNotBlank();
        return readMap(message);
    }

    /**
     * Waits for a message, because publication happens after the transaction commits and
     * therefore some time after the service call has returned.
     */
    private Message poll(String queue, Duration timeout) {
        return rabbit.receive(queue, timeout.toMillis());
    }

    /**
     * Asserts that nothing is published. Uses a short window deliberately: a long one would
     * make every such test slow, and a publication that has not happened within a second of
     * the commit is not going to.
     */
    private void assertNothingPublished(String queue) {
        assertThat(poll(queue, Duration.ofSeconds(1)))
                .as("no message should have been published to %s", queue)
                .isNull();
    }

    private void drain(String queue) {
        while (rabbit.receive(queue) != null) {
            // discard
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(Message message) {
        return json.readValue(message.getBody(), Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadOf(Map<String, Object> envelope) {
        return (Map<String, Object>) envelope.get("payload");
    }

}
