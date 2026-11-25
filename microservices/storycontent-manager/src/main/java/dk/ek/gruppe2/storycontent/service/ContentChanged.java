package dk.ek.gruppe2.storycontent.service;

/**
 * An in-process note that something was changed and needs republishing.
 *
 * <p>Not the message that goes on the broker. It is a Spring application event, raised
 * inside the transaction that made the change and handled only after that transaction
 * commits. That ordering is the whole point of the class.
 *
 * <p><b>Why not publish straight to RabbitMQ from the service method.</b> The broker is
 * not part of the database transaction, so a message sent before the commit is already
 * gone if the commit then fails. The consumer would hold a scene, with a version, that we
 * never saved — and because consumers discard anything not newer than what they have,
 * every later message for that scene would be thrown away too. One rollback would freeze
 * that scene in the read model for good.
 *
 * <p><b>What the chosen order costs.</b> If the process dies between committing and
 * publishing, the change is saved and nobody is told; the read model stays stale until
 * that scene is edited again. Recoverable, unlike the other way round.
 *
 * <p><b>The proper fix is an outbox:</b> write the message into a table in the same
 * transaction, and let a separate reader drain it to the broker. Not implemented — a
 * known gap rather than an oversight.
 */
sealed interface ContentChanged {

    record Scene(Integer sceneId) implements ContentChanged {
    }

    record Item(Integer itemId) implements ContentChanged {
    }

    record Race(Integer raceId) implements ContentChanged {
    }
}
