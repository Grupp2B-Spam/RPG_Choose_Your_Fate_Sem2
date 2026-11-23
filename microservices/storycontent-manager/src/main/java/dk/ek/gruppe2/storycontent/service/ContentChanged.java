package dk.ek.gruppe2.storycontent.service;

/**
 * An in-process note that a published aggregate changed and has to be republished.
 *
 * <p>This is not the event that goes on the broker. It is a Spring application event,
 * raised inside the transaction that made the change and handled only once that
 * transaction has committed. The distinction is the point of the class.
 *
 * <p><b>Why not publish straight to RabbitMQ from the service method.</b> The write and
 * the publish would then be in the same transaction only from our point of view: AMQP is
 * not part of the database transaction, so a message sent before the commit is already
 * gone if the commit then fails. The consumer would hold a scene, with a version, that
 * this service does not have. That is worse than losing the message, because every later
 * event for that scene carries a lower version and is therefore discarded as stale —
 * one rollback would silently freeze that scene in the read model.
 *
 * <p><b>What this costs instead.</b> Publishing after the commit moves the failure to the
 * other side: if the process dies between committing and publishing, the change is saved
 * and no one is told. The read model is then stale until the next edit of that scene. That
 * is recoverable and detectable, where the other way round is neither.
 *
 * <p><b>The real answer is an outbox</b> — write the message into a table in the same
 * transaction as the change, and have a separate reader drain it to the broker, which
 * makes the two atomic. It is not implemented here, and it is recorded as a known gap
 * rather than left to be discovered: see {@code OPEN-DESIGN-ISSUES.md}.
 */
sealed interface ContentChanged {

    record Scene(Integer sceneId) implements ContentChanged {
    }

    record Item(Integer itemId) implements ContentChanged {
    }

    record Race(Integer raceId) implements ContentChanged {
    }
}
