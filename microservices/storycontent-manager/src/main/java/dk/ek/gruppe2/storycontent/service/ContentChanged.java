package dk.ek.gruppe2.storycontent.service;

/**
 * An in-process note that something was changed and needs republishing. Not the message
 * that goes on the broker: a Spring application event, raised inside the transaction and
 * handled only after it commits.
 *
 * Publishing straight to RabbitMQ from the service method would put a message on the broker
 * that survives a rollback. The consumer would hold a version this service never saved, and
 * would then discard every later event for that entity as stale.
 *
 * The cost of this order: if the process dies between the commit and the publish, the change
 * is saved and nobody is told. The fix is an outbox, which is not implemented.
 */
sealed interface ContentChanged {

    record Scene(Integer sceneId) implements ContentChanged {
    }

    record Item(Integer itemId) implements ContentChanged {
    }

    record Race(Integer raceId) implements ContentChanged {
    }
}
