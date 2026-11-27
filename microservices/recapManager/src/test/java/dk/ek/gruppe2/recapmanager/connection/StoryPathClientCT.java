package dk.ek.gruppe2.recapmanager.connection;

import dk.ek.gruppe2.recapmanager.config.StoryPathProperties;
import dk.ek.gruppe2.recapmanager.grpc.storypath.v1.*;
import dk.ek.gruppe2.recapmanager.storypath.GrpcJourneyHistoryClient;
import io.grpc.*;
import io.grpc.inprocess.*;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.assertj.core.api.Assertions.*;

class StoryPathClientCT {
    private Server server;
    private ManagedChannel channel;
    private final List<String> tokens = new ArrayList<>();

    @AfterEach void shutdown() {
        SecurityContextHolder.clearContext();
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    @Test void eachPageUsesTheCallerTokenAndTheNextRequestUsesItsOwnToken() throws Exception {
        var client = start(new StoryPathServiceGrpc.StoryPathServiceImplBase() {
            @Override public void getChoicesAfter(GetChoicesAfterRequest request,
                                                  StreamObserver<GetChoicesAfterResponse> response) {
                long sequence = request.getAfterSequence() + 1;
                response.onNext(GetChoicesAfterResponse.newBuilder().setCharacterId(1).setSnapshotSequence(2)
                        .addChoices(PathChoice.newBuilder().setSequence(sequence).setChoiceId((int) sequence)
                                .setDescription("A journey entry"))
                        .setHasMore(sequence < 2).build());
                response.onCompleted();
            }
        });
        authenticate("first-token");
        assertThat(client.fetchAfter(1, 0)).hasSize(2);
        authenticate("second-token");
        assertThat(client.fetchAfter(1, 0)).hasSize(2);
        assertThat(tokens).containsExactly("Bearer first-token", "Bearer first-token",
                "Bearer second-token", "Bearer second-token");
    }

    @Test void missingAuthenticationMakesNoRpc() throws Exception {
        var client = start(new StoryPathServiceGrpc.StoryPathServiceImplBase() {});
        assertThatThrownBy(() -> client.fetchAfter(1, 0)).isInstanceOf(AccessDeniedException.class);
        assertThat(tokens).isEmpty();
    }

    @Test void unauthenticatedRpcIsNotReportedAsAServiceOutage() throws Exception {
        var client = failingClient(Status.UNAUTHENTICATED);
        authenticate("token");
        assertThatThrownBy(() -> client.fetchAfter(1, 0)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test void deniedRpcIsNotReportedAsAServiceOutage() throws Exception {
        var client = failingClient(Status.PERMISSION_DENIED);
        authenticate("token");
        assertThatThrownBy(() -> client.fetchAfter(1, 0)).isInstanceOf(AccessDeniedException.class);
    }

    private GrpcJourneyHistoryClient failingClient(Status status) throws Exception {
        return start(new StoryPathServiceGrpc.StoryPathServiceImplBase() {
            @Override public void getChoicesAfter(GetChoicesAfterRequest request,
                                                  StreamObserver<GetChoicesAfterResponse> response) {
                response.onError(status.asRuntimeException());
            }
        });
    }

    private GrpcJourneyHistoryClient start(BindableService service) throws Exception {
        String name = InProcessServerBuilder.generateName();
        ServerInterceptor interceptor = new ServerInterceptor() {
            @Override public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                    Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                tokens.add(headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));
                return next.startCall(call, headers);
            }
        };
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(ServerInterceptors.intercept(service, interceptor)).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        return new GrpcJourneyHistoryClient(StoryPathServiceGrpc.newBlockingStub(channel), new StoryPathProperties());
    }

    private void authenticate(String value) {
        var jwt = Jwt.withTokenValue(value).header("alg", "RS256").subject("player-1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }
}
