package dk.ek.gruppe2.recapmanager.connection;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.*;
import dk.ek.gruppe2.recapmanager.speech.*;
import io.grpc.*;
import io.grpc.inprocess.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class GrpcSpeechClientCT {
    private Server server;
    private ManagedChannel channel;

    @AfterEach void shutdown() {
        SecurityContextHolder.clearContext();
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    @Test void grpcForwardsOptionsHashWaitAndBearerMetadata() throws Exception {
        var received = new AtomicReference<SpeechRequest>();
        var authorization = new AtomicReference<String>();
        var service = new SpeechServiceGrpc.SpeechServiceImplBase() {
            @Override public void getOrCreateSpeech(SpeechRequest request, StreamObserver<SpeechResponse> response) {
                received.set(request);
                response.onNext(SpeechResponse.newBuilder().setContentHash(request.getContentHash())
                        .setStatus(SpeechStatus.READY).setAudioUrl("https://audio.example.test/one.mp3").build());
                response.onCompleted();
            }
        };
        ServerInterceptor interceptor = new ServerInterceptor() {
            @Override public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                    Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                authorization.set(headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));
                return next.startCall(call, headers);
            }
        };
        var properties = new SpeechProperties();
        var client = start(ServerInterceptors.intercept(service, interceptor), properties);
        authenticate();
        var request = SpeechContent.request("A journey", properties);
        assertThat(client.getOrCreate(request).getStatus()).isEqualTo(SpeechStatus.READY);
        assertThat(received.get()).isEqualTo(request);
        assertThat(authorization.get()).isEqualTo("Bearer example-token");
    }

    @Test
    @Timeout(10)
    void anUnresponsiveServiceIsBoundedByTheGrpcDeadline() throws Exception {
        var properties = new SpeechProperties();
        properties.setWaitMillis(1000);
        properties.setDeadlineGraceMillis(1000);
        var requestArrived = new CountDownLatch(1);
        var client = start(new SpeechServiceGrpc.SpeechServiceImplBase() {
            @Override public void getOrCreateSpeech(SpeechRequest request, StreamObserver<SpeechResponse> response) {
                requestArrived.countDown();
                // Work was received but does not finish; the client must enforce its own deadline.
            }
        }, properties);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var response = executor.submit(() -> {
                authenticate();
                try { return client.getOrCreate(SpeechContent.request("A journey", properties)); }
                finally { SecurityContextHolder.clearContext(); }
            });
            assertThat(requestArrived.await(3, TimeUnit.SECONDS)).as("server received the request").isTrue();
            assertThatThrownBy(() -> response.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(StatusRuntimeException.class)
                    .satisfies(error -> assertThat(((StatusRuntimeException) error.getCause()).getStatus().getCode())
                            .isEqualTo(Status.Code.DEADLINE_EXCEEDED));
        } finally {
            channel.shutdownNow();
            executor.shutdownNow();
        }
    }

    @Test void unauthenticatedRetrievalNeverCallsTheSender() throws Exception {
        var received = new AtomicInteger();
        var client = start(new SpeechServiceGrpc.SpeechServiceImplBase() {
            @Override public void getOrCreateSpeech(SpeechRequest request, StreamObserver<SpeechResponse> response) {
                received.incrementAndGet();
                response.onError(Status.UNAUTHENTICATED.asRuntimeException());
            }
        }, new SpeechProperties());
        assertThatThrownBy(() -> client.getOrCreate(SpeechContent.request("A journey", new SpeechProperties())))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(received.get()).isZero();
    }

    @Test void downstreamTokenRejectionRemainsAnAuthenticationFailure() throws Exception {
        var client = failingClient(Status.UNAUTHENTICATED);
        authenticate();
        assertThatThrownBy(() -> client.getOrCreate(SpeechContent.request("A journey", new SpeechProperties())))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test void downstreamPermissionDenialRemainsAnAuthorizationFailure() throws Exception {
        var client = failingClient(Status.PERMISSION_DENIED);
        authenticate();
        assertThatThrownBy(() -> client.getOrCreate(SpeechContent.request("A journey", new SpeechProperties())))
                .isInstanceOf(AccessDeniedException.class);
    }

    private GrpcSpeechClient failingClient(Status status) throws Exception {
        return start(new SpeechServiceGrpc.SpeechServiceImplBase() {
            @Override public void getOrCreateSpeech(SpeechRequest request, StreamObserver<SpeechResponse> response) {
                response.onError(status.asRuntimeException());
            }
        }, new SpeechProperties());
    }

    private GrpcSpeechClient start(BindableService service, SpeechProperties properties) throws Exception {
        return start(service.bindService(), properties);
    }

    private GrpcSpeechClient start(ServerServiceDefinition service, SpeechProperties properties) throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(service).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        return new GrpcSpeechClient(SpeechServiceGrpc.newBlockingStub(channel), properties);
    }

    private void authenticate() {
        var jwt = Jwt.withTokenValue("example-token").header("alg", "RS256").subject("player-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, java.util.List.of()));
    }
}
