package dk.ek.gruppe2.storycontent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dk.ek.gruppe2.storycontent.messaging.EventPublisher;
import dk.ek.gruppe2.storycontent.messaging.RabbitConfig;
import dk.ek.gruppe2.storycontent.repository.ItemRepository;
import dk.ek.gruppe2.storycontent.repository.RaceRepository;
import dk.ek.gruppe2.storycontent.repository.SceneRepository;

/**
 * Publishes an aggregate to the broker once the transaction that changed it has committed.
 *
 * A service method does not publish; it says that something changed, and this listens. The
 * reasoning for the split is in ContentChanged.
 *
 * Each handler re-reads the aggregate in a new transaction, so what goes out is the
 * committed state. If a second author has saved since, the re-read picks up their version,
 * which is newer content under a newer number.
 *
 * A failure to publish is logged and swallowed. The transaction has already committed, so
 * there is nothing left to roll back.
 */
@Component
public class ContentEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ContentEventDispatcher.class);

    private final ApplicationEventPublisher applicationEvents;
    private final EventPublisher eventPublisher;
    private final PayloadAssembler assembler;
    private final SceneRepository scenes;
    private final ItemRepository items;
    private final RaceRepository races;

    public ContentEventDispatcher(
            ApplicationEventPublisher applicationEvents,
            EventPublisher eventPublisher,
            PayloadAssembler assembler,
            SceneRepository scenes,
            ItemRepository items,
            RaceRepository races) {
        this.applicationEvents = applicationEvents;
        this.eventPublisher = eventPublisher;
        this.assembler = assembler;
        this.scenes = scenes;
        this.items = items;
        this.races = races;
    }

    /** Called from inside a transaction. Nothing is sent until that transaction commits. */
    public void sceneChanged(Integer sceneId) {
        applicationEvents.publishEvent(new ContentChanged.Scene(sceneId));
    }

    public void itemChanged(Integer itemId) {
        applicationEvents.publishEvent(new ContentChanged.Item(itemId));
    }

    public void raceChanged(Integer raceId) {
        applicationEvents.publishEvent(new ContentChanged.Race(raceId));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onSceneChanged(ContentChanged.Scene changed) {
        try {
            scenes.findByIdForPublishing(changed.sceneId())
                    .ifPresentOrElse(
                            scene -> eventPublisher.publish(
                                    RabbitConfig.ROUTING_SCENE_CONTENT_UPDATED,
                                    assembler.scene(scene)),
                            () -> log.warn("scene {} was deleted before it could be published",
                                    changed.sceneId()));
        } catch (RuntimeException e) {
            log.error("failed to publish scene {}; the change is committed but consumers "
                    + "have not been told, and will stay stale until this scene is edited "
                    + "again", changed.sceneId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onItemChanged(ContentChanged.Item changed) {
        try {
            items.findById(changed.itemId())
                    .ifPresentOrElse(
                            item -> eventPublisher.publish(
                                    RabbitConfig.ROUTING_ITEM_DEFINITION_UPDATED,
                                    assembler.item(item)),
                            () -> log.warn("item {} was deleted before it could be published",
                                    changed.itemId()));
        } catch (RuntimeException e) {
            log.error("failed to publish item {}; the change is committed but consumers "
                    + "have not been told", changed.itemId(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onRaceChanged(ContentChanged.Race changed) {
        try {
            races.findById(changed.raceId())
                    .ifPresentOrElse(
                            race -> eventPublisher.publish(
                                    RabbitConfig.ROUTING_RACE_DEFINITION_UPDATED,
                                    assembler.race(race)),
                            () -> log.warn("race {} was deleted before it could be published",
                                    changed.raceId()));
        } catch (RuntimeException e) {
            log.error("failed to publish race {}; the change is committed but consumers "
                    + "have not been told", changed.raceId(), e);
        }
    }
}
