package dk.ek.gruppe2.recapmanager.storypath;

import dk.ek.gruppe2.recapmanager.config.StoryPathProperties;
import dk.ek.gruppe2.recapmanager.exception.StoryPathException;
import dk.ek.gruppe2.recapmanager.grpc.storypath.v1.GetChoicesAfterRequest;
import dk.ek.gruppe2.recapmanager.grpc.storypath.v1.StoryPathServiceGrpc;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrpcJourneyHistoryClient implements JourneyHistoryClient {
    private final StoryPathServiceGrpc.StoryPathServiceBlockingStub stub;
    private final StoryPathProperties properties;

    public GrpcJourneyHistoryClient(StoryPathServiceGrpc.StoryPathServiceBlockingStub stub, StoryPathProperties properties) {
        this.stub = stub;
        this.properties = properties;
    }

    @Override
    public List<JourneyChoice> fetchAfter(int characterId, long afterSequence) {
        if (characterId <= 0 || afterSequence < 0) throw new IllegalArgumentException("Invalid character/checkpoint");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated JWT is required for StoryPathManager");
        }
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + token.getToken().getTokenValue());
        var authenticatedStub = stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
        List<JourneyChoice> choices = new ArrayList<>();
        long cursor = afterSequence;
        long snapshot = 0;
        try {
            for (int page = 0; page < properties.getMaxPages(); page++) {
                var request = GetChoicesAfterRequest.newBuilder().setCharacterId(characterId)
                        .setAfterSequence(cursor).setPageSize(properties.getPageSize())
                        .setSnapshotSequence(snapshot).build();
                var response = authenticatedStub.withDeadlineAfter(properties.getDeadlineMillis(), TimeUnit.MILLISECONDS)
                        .getChoicesAfter(request);
                if (response.getCharacterId() != characterId || response.getSnapshotSequence() < cursor
                        || (page > 0 && response.getSnapshotSequence() != snapshot)
                        || response.getChoicesCount() > properties.getPageSize()) {
                    throw new StoryPathException("StoryPathManager returned an invalid snapshot");
                }
                snapshot = response.getSnapshotSequence();
                for (var choice : response.getChoicesList()) {
                    if (choice.getSequence() <= cursor || choice.getSequence() > snapshot) {
                        throw new StoryPathException("StoryPathManager returned unordered/replayed entries");
                    }
                    choices.add(new JourneyChoice(choice.getSequence(), choice.getChoiceId(), choice.getFromScene(),
                            choice.getToScene(), choice.getDescription(), choice.getConsequence(), choice.getStoryWeight()));
                    cursor = choice.getSequence();
                }
                if (!response.getHasMore()) {
                    if (cursor != snapshot) throw new StoryPathException("StoryPathManager returned an incomplete snapshot");
                    return List.copyOf(choices);
                }
                if (response.getChoicesCount() == 0 || cursor >= snapshot) {
                    throw new StoryPathException("StoryPathManager pagination made no progress");
                }
            }
            throw new StoryPathException("StoryPathManager exceeded the configured page limit");
        } catch (StatusRuntimeException exception) {
            switch (exception.getStatus().getCode()) {
                case UNAUTHENTICATED -> throw new InvalidBearerTokenException("StoryPathManager rejected the access token");
                case PERMISSION_DENIED -> throw new AccessDeniedException("StoryPathManager denied access to this journey");
                default -> { }
            }
            // Unavailability is not an empty history: do not silently claim a stale summary is current.
            throw new StoryPathException("Could not retrieve new journey entries from StoryPathManager", exception);
        } catch (IllegalArgumentException exception) {
            throw new StoryPathException("StoryPathManager returned malformed journey entries", exception);
        }
    }
}
