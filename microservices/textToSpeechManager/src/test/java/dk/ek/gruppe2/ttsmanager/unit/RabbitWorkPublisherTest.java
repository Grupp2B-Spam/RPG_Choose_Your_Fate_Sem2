package dk.ek.gruppe2.ttsmanager.unit;

import dk.ek.gruppe2.ttsmanager.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;

class RabbitWorkPublisherTest {
    private final SpeechSpec spec = new SpeechSpec(SpeechSpec.hash("Journey", "voice", "model", "mp3_44100_128"),
            "Journey", "voice", "model", "mp3_44100_128");

    @Test void confirmedWorkReferencesTheStoredTtsJobWithoutCopyingSynthesisSettings() {
        var json = JsonMapper.builder().build();
        var rabbit = new RabbitTemplate() {
            @Override public void send(String exchange, String key, Message message, CorrelationData confirmation) {
                assertThat(exchange).isEmpty();
                assertThat(key).isEqualTo("tts.work");
                var body = json.readTree(message.getBody());
                assertThat(body.path("contentHash").asString()).isEqualTo(spec.contentHash());
                for (String field : new String[]{"text", "voiceId", "model", "outputFormat"}) {
                    assertThat(body.has(field)).isFalse();
                }
                assertThat(body.path("eventVersion").asInt()).isEqualTo(2);
                assertThat(body.path("eventType").asString()).isEqualTo("SpeechRequested");
                assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
                assertThat(message.getMessageProperties().getMessageId()).isEqualTo(body.path("eventId").asString());
                confirmation.getFuture().complete(new CorrelationData.Confirm(true, null));
            }
        };
        assertThatCode(() -> new RabbitWorkPublisher(rabbit, json, "tts.work", 100).publish(spec)).doesNotThrowAnyException();
    }

    @Test void brokerNackCannotBeReportedAsAcceptedWork() {
        rejected(false);
    }

    @Test void acknowledgedButUnroutableWorkCannotBeReportedAsAccepted() {
        rejected(true);
    }

    private void rejected(boolean unroutable) {
        var rabbit = new RabbitTemplate() {
            @Override public void send(String exchange, String key, Message message, CorrelationData confirmation) {
                if (unroutable) confirmation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", exchange, key));
                confirmation.getFuture().complete(new CorrelationData.Confirm(unroutable, "rejected"));
            }
        };
        assertThatThrownBy(() -> new RabbitWorkPublisher(rabbit, JsonMapper.builder().build(), "tts.work", 100).publish(spec))
                .isInstanceOf(IllegalStateException.class);
    }
}
