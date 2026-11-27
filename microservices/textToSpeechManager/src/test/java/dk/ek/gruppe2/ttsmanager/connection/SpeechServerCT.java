package dk.ek.gruppe2.ttsmanager.connection;

import com.linecorp.armeria.server.Server;
import dk.ek.gruppe2.ttsmanager.*;
import dk.ek.gruppe2.ttsmanager.grpc.speech.v1.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class SpeechServerCT {
    private WorkflowFixture f;
    private Server server;
    private ManagedChannel channel;
    private HttpClient http;

    @BeforeEach void start() {
        f = new WorkflowFixture();
        server = SpeechServer.build(0, new SpeechRpc(f.lookup, f.policy), f.security, f.events);
        server.start().join();
        channel = ManagedChannelBuilder.forAddress("127.0.0.1", server.activeLocalPort()).usePlaintext().build();
        http = HttpClient.newHttpClient();
    }
    @AfterEach void stop() throws Exception {
        channel.shutdownNow().awaitTermination(3, TimeUnit.SECONDS);
        http.close();
        server.stop().join();
    }

    @Test void grpcMissThenHttpQueueDeliveryThenGrpcHitShareOnePortAndOneAudioIdentity() throws Exception {
        var pending = caller().getOrCreateSpeech(request());
        assertThat(pending.getStatus()).isEqualTo(SpeechStatus.PENDING);
        assertThat(pending.getContentHash()).isEqualTo(f.spec.contentHash());
        assertThat(f.publications).containsExactly(f.spec);
        assertThat(event("delivery-token", f.eventJson()).statusCode()).isEqualTo(204);
        assertThat(event("delivery-token", f.eventJson()).statusCode()).isEqualTo(204);
        var ready = caller().getOrCreateSpeech(request());
        assertThat(ready.getStatus()).isEqualTo(SpeechStatus.READY);
        assertThat(ready.getContentHash()).isEqualTo(f.spec.contentHash());
        assertThat(ready.getAudioUrl()).contains(f.spec.contentHash());
        assertThat(f.providerCalls).hasValue(1);
    }

    @Test void invalidTextOrWaitCannotCreateOrPublishSpeechWork() {
        for (var invalid : java.util.List.of(SpeechRequest.newBuilder().setText(" ").build(),
                SpeechRequest.newBuilder().setText(f.spec.text()).setWaitMillis(5001).build())) {
            assertThatThrownBy(() -> caller().getOrCreateSpeech(invalid))
                    .isInstanceOf(StatusRuntimeException.class)
                    .satisfies(error -> assertThat(((StatusRuntimeException) error).getStatus().getCode())
                            .isEqualTo(Status.Code.INVALID_ARGUMENT));
        }
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM speech_job", Integer.class)).isZero();
        assertThat(f.publications).isEmpty();
    }

    @Test void unauthenticatedGrpcRequestCannotCreateOrPublishWork() {
        assertThatThrownBy(() -> SpeechServiceGrpc.newBlockingStub(channel).withDeadlineAfter(3, TimeUnit.SECONDS)
                .getOrCreateSpeech(request())).isInstanceOf(StatusRuntimeException.class)
                .satisfies(error -> assertThat(((StatusRuntimeException) error).getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED));
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM speech_job", Integer.class)).isZero();
        assertThat(f.publications).isEmpty();
    }

    @Test void playerJwtCannotImpersonateTheEventDeliveryComponent() throws Exception {
        assertThat(event("caller-token", f.eventJson()).statusCode()).isEqualTo(401);
        assertThat(f.providerCalls).hasValue(0);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM speech_job", Integer.class)).isZero();
    }

    @Test void emptyEventTextCannotTriggerPaidGeneration() throws Exception {
        assertThat(event("delivery-token", f.eventJson().replace("A journey", " ")).statusCode()).isEqualTo(400);
        assertThat(f.providerCalls).hasValue(0);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM speech_job", Integer.class)).isZero();
    }

    private SpeechServiceGrpc.SpeechServiceBlockingStub caller() {
        var metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer caller-token");
        return SpeechServiceGrpc.newBlockingStub(channel).withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(3, TimeUnit.SECONDS);
    }
    private SpeechRequest request() {
        return SpeechRequest.newBuilder().setText(f.spec.text()).setWaitMillis(0).build();
    }
    private HttpResponse<String> event(String token, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.activeLocalPort() + "/events/recap"))
                .timeout(Duration.ofSeconds(5)).header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json").header("ce-specversion", "1.0")
                .header("ce-id", "delivery-1").header("ce-source", "/test/rabbitmq").header("ce-type", "dev.knative.rabbitmq.event")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
