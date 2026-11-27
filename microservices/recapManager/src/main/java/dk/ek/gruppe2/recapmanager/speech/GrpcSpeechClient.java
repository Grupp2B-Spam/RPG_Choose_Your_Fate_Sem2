package dk.ek.gruppe2.recapmanager.speech;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.*;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import java.util.concurrent.TimeUnit;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

public class GrpcSpeechClient implements SpeechClient {
    private final SpeechServiceGrpc.SpeechServiceBlockingStub stub;
    private final SpeechProperties properties;

    public GrpcSpeechClient(SpeechServiceGrpc.SpeechServiceBlockingStub stub, SpeechProperties properties) {
        this.stub = stub;
        this.properties = properties;
    }

    @Override
    public SpeechResponse getOrCreate(SpeechRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated JWT is required for speech retrieval");
        }
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer " + token.getToken().getTokenValue());
        try {
            return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(request.getWaitMillis() + properties.getDeadlineGraceMillis(), TimeUnit.MILLISECONDS)
                .getOrCreateSpeech(request);
        } catch (StatusRuntimeException exception) {
            switch (exception.getStatus().getCode()) {
                case UNAUTHENTICATED -> throw new InvalidBearerTokenException("TextToSpeechManager rejected the access token");
                case PERMISSION_DENIED -> throw new AccessDeniedException("TextToSpeechManager denied access to speech");
                default -> throw exception;
            }
        }
    }
}
