package dk.ek.gruppe2.ttsmanager;

import dk.ek.gruppe2.ttsmanager.grpc.speech.v1.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

public final class SpeechRpc extends SpeechServiceGrpc.SpeechServiceImplBase {
    private final SpeechLookup lookup;
    private final SpeechPolicy policy;

    public SpeechRpc(SpeechLookup lookup, SpeechPolicy policy) { this.lookup = lookup; this.policy = policy; }

    @Override public void getOrCreateSpeech(SpeechRequest request, StreamObserver<SpeechResponse> response) {
        try {
            var spec = policy.specification(request.getText());
            policy.validateWait(request.getWaitMillis());
            var result = lookup.getOrCreate(spec, request.getWaitMillis());
            response.onNext(SpeechResponse.newBuilder().setContentHash(spec.contentHash())
                    .setStatus(SpeechStatus.valueOf(result.status())).setAudioUrl(result.audioUrl()).build());
            response.onCompleted();
        } catch (IllegalArgumentException invalid) {
            response.onError(Status.INVALID_ARGUMENT.withDescription("Invalid speech request").asRuntimeException());
        } catch (RuntimeException unavailable) {
            response.onError(Status.UNAVAILABLE.withDescription("Speech dependencies unavailable").asRuntimeException());
        }
    }

    public static ServerInterceptor authentication(JwtSecurity security) {
        return new ServerInterceptor() {
            @Override public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                    Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                try {
                    security.requireCaller(headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)));
                    return next.startCall(call, headers);
                } catch (JwtSecurity.Rejected rejected) {
                    call.close(rejected.authenticated() ? Status.PERMISSION_DENIED : Status.UNAUTHENTICATED, new Metadata());
                    return new ServerCall.Listener<>() {};
                }
            }
        };
    }
}
