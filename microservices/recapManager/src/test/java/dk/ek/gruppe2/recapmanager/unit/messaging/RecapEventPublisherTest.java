package dk.ek.gruppe2.recapmanager.unit.messaging;

import dk.ek.gruppe2.recapmanager.config.RecapMessagingProperties;
import dk.ek.gruppe2.recapmanager.messaging.RecapEventPublisher;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import dk.ek.gruppe2.recapmanager.model.RecapOutboxMessage;
import dk.ek.gruppe2.recapmanager.repository.RecapOutboxRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecapEventPublisherTest {
    private final RecapOutboxRepository repository = mock(RecapOutboxRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RecapMessagingProperties properties = new RecapMessagingProperties();
    private final RecapEventPublisher publisher = new RecapEventPublisher(repository, rabbit, properties);

    @Test void confirmedPersistentMessageIsRemovedFromOutbox() {
        var event = event();
        doAnswer(call -> {
            Message message = call.getArgument(2);
            CorrelationData data = call.getArgument(3);
            assertThat(message.getMessageProperties().getMessageId()).isEqualTo(event.getId());
            assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
            assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publish(event);
        verify(repository).delete(event);
        verify(repository, never()).save(any());
    }

    @Test void nackRetainsTheSameEventIdForRetry() {
        var event = event();
        var messages = new ArrayList<Message>();
        var correlationIds = new ArrayList<String>();
        doAnswer(call -> {
            messages.add(call.getArgument(2));
            CorrelationData data = call.getArgument(3);
            correlationIds.add(data.getId());
            boolean retry = messages.size() == 2;
            data.getFuture().complete(new CorrelationData.Confirm(retry, retry ? null : "nack"));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        Instant beforeAttempt = Instant.now();
        publish(event);
        verify(repository).save(event);
        verify(repository, never()).delete(any());
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfterOrEqualTo(beforeAttempt.plusMillis(properties.getRetryDelayMillis()));

        // Simulate the next poll loading the persisted event rather than reusing the same Java object.
        var reloaded = new RecapOutboxMessage(event.getId(), event.getPayload(), event.getNextAttemptAt());
        publish(reloaded);
        verify(repository).delete(reloaded);
        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(message -> message.getMessageProperties().getMessageId())
                .containsExactly(event.getId(), event.getId());
        assertThat(correlationIds).containsExactly(event.getId(), event.getId());
        byte[] expectedPayload = event.getPayload().getBytes(StandardCharsets.UTF_8);
        assertThat(messages.getFirst().getBody()).isEqualTo(expectedPayload);
        assertThat(messages.getLast().getBody()).isEqualTo(expectedPayload);
    }

    @Test void unroutableMessageIsRetainedEvenWhenExchangeAcknowledgesIt() {
        var event = event();
        doAnswer(call -> {
            CorrelationData data = call.getArgument(3);
            data.setReturned(new ReturnedMessage(call.getArgument(2), 312, "NO_ROUTE", "exchange", "key"));
            data.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publish(event);
        verify(repository).save(event);
        verify(repository, never()).delete(any());
    }

    @Test void connectionFailureRetainsThePublicationIntent() {
        var event = event();
        doThrow(new IllegalStateException("broker unavailable")).when(rabbit)
                .send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
        publish(event);
        verify(repository).save(event);
        verify(repository, never()).delete(any());
    }

    private void publish(RecapOutboxMessage event) {
        when(repository.findTop25ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any())).thenReturn(List.of(event));
        publisher.publishPending();
    }

    private RecapOutboxMessage event() {
        return new RecapOutboxMessage("stable-event-id", "{\"eventType\":\"RecapGenerated\"}", Instant.now());
    }
}
