package dk.ek.gruppe2.recapmanager;

import dk.ek.gruppe2.recapmanager.config.StoryPathProperties;
import dk.ek.gruppe2.recapmanager.exception.StoryPathException;
import dk.ek.gruppe2.recapmanager.grpc.storypath.v1.*;
import dk.ek.gruppe2.recapmanager.storypath.GrpcJourneyHistoryClient;
import io.grpc.*;
import io.grpc.inprocess.*;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import static org.assertj.core.api.Assertions.*;

class GrpcJourneyHistoryClientTest {
    final StoryPathProperties properties = new StoryPathProperties();
    final List<GetChoicesAfterRequest> requests = new ArrayList<>();
    Function<GetChoicesAfterRequest, GetChoicesAfterResponse> responder;
    ManagedChannel channel;
    Server server;
    GrpcJourneyHistoryClient client;
    String receivedAuthorization;

    @BeforeEach
    void startFakeServer() throws Exception {
        properties.setPageSize(2);
        String name = InProcessServerBuilder.generateName();
        var service = new StoryPathServiceGrpc.StoryPathServiceImplBase() {
            @Override public void getChoicesAfter(GetChoicesAfterRequest request, StreamObserver<GetChoicesAfterResponse> observer) {
                requests.add(request);
                try {
                    GetChoicesAfterResponse result = responder.apply(request);
                    if (result != null) { observer.onNext(result); observer.onCompleted(); }
                } catch (StatusRuntimeException exception) { observer.onError(exception); }
            }
        };
        ServerInterceptor authentication = new ServerInterceptor() {
            @Override public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                    Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                receivedAuthorization = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
                return next.startCall(call, headers);
            }
        };
        server = InProcessServerBuilder.forName(name).directExecutor().addService(service).intercept(authentication).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new GrpcJourneyHistoryClient(StoryPathServiceGrpc.newBlockingStub(channel), properties);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("mock-test-token").header("alg", "RS256").subject("player-1").build(), List.of()));
    }

    @AfterEach
    void stopServer() throws Exception {
        SecurityContextHolder.clearContext();
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void actualGrpcCallsUseExclusiveCursorFixedSnapshotAndForwardToken() {
        responder = request -> request.getAfterSequence() == 7
                ? response(42, 10, true, 8, 9) : response(42, 10, false, 10);
        assertThat(client.fetchAfter(42, 7)).extracting(choice -> choice.sequence()).containsExactly(8L, 9L, 10L);
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getAfterSequence()).isEqualTo(7);
        assertThat(requests.get(0).getSnapshotSequence()).isZero();
        assertThat(requests.get(1).getAfterSequence()).isEqualTo(9);
        assertThat(requests.get(1).getSnapshotSequence()).isEqualTo(10);
        assertThat(requests.get(0).getPageSize()).isEqualTo(2);
        assertThat(receivedAuthorization).isEqualTo("Bearer mock-test-token");
    }

    @Test
    void emptyResponseAtSavedCheckpointIsANoOp() {
        responder = request -> response(42, 7, false);
        assertThat(client.fetchAfter(42, 7)).isEmpty();
    }

    @Test
    void mismatchedCharacterReplayMissingEntriesAndInvalidPaginationAreRejected() {
        for (var invalid : List.of(response(99, 8, false, 8), response(42, 8, false, 7),
                response(42, 9, false, 8), response(42, 9, true), response(42, 8, true, 8),
                response(42, 9, false, 9, 8), response(42, 8, false, 9))) {
            responder = request -> invalid;
            assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(StoryPathException.class);
        }
    }

    @Test
    void snapshotCannotMoveBetweenPages() {
        responder = request -> request.getAfterSequence() == 7
                ? response(42, 10, true, 8, 9) : response(42, 11, false, 10, 11);
        assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(StoryPathException.class);
    }

    @Test
    void serverFailureIsNotTreatedAsEmptyHistory() {
        responder = request -> { throw Status.UNAVAILABLE.asRuntimeException(); };
        assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(StoryPathException.class)
                .hasCauseInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void stalledServerHitsDeadline() {
        properties.setDeadlineMillis(50);
        responder = request -> null; // Leave the RPC open; the real gRPC deadline must end it.
        assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(StoryPathException.class)
                .satisfies(exception -> assertThat(Status.fromThrowable(exception.getCause()).getCode())
                        .isEqualTo(Status.Code.DEADLINE_EXCEEDED));
    }

    @Test
    void pageLimitDoesNotReturnPartialHistory() {
        properties.setMaxPages(1);
        responder = request -> response(42, 10, true, 8, 9);
        assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(StoryPathException.class);
    }

    @Test
    void unauthenticatedCallDoesNotContactTheServer() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> client.fetchAfter(42, 7)).isInstanceOf(AccessDeniedException.class);
        assertThat(requests).isEmpty();
    }

    private GetChoicesAfterResponse response(int characterId, long snapshot, boolean hasMore, long... sequences) {
        var result = GetChoicesAfterResponse.newBuilder().setCharacterId(characterId)
                .setSnapshotSequence(snapshot).setHasMore(hasMore);
        for (long sequence : sequences) {
            result.addChoices(PathChoice.newBuilder().setSequence(sequence).setChoiceId(10)
                    .setFromScene("Village").setToScene("Bridge").setDescription("Helped a traveler")
                    .setConsequence("Gained an ally").setStoryWeight(5));
        }
        return result.build();
    }
}
