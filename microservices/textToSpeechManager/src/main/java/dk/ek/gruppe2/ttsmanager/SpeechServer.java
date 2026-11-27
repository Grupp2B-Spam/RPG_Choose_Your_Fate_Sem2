package dk.ek.gruppe2.ttsmanager;

import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.grpc.GrpcService;
import io.grpc.ServerInterceptors;
import java.time.Duration;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** One Knative application port for gRPC, event delivery and health checks. */
@Component
public final class SpeechServer implements SmartLifecycle {
    private final Server server;
    private volatile boolean running;

    public SpeechServer(SpeechRpc rpc, JwtSecurity security, SpeechEvents events, Environment env) {
        server = build(TtsConfiguration.number(env, "tts.port", 1, 65535), rpc, security, events);
    }

    public static Server build(int port, SpeechRpc rpc, JwtSecurity security, SpeechEvents events) {
        return Server.builder().http(port).maxRequestLength(262144).requestTimeout(Duration.ofSeconds(120))
                .gracefulShutdownTimeout(Duration.ofSeconds(1), Duration.ofSeconds(125))
                .service(GrpcService.builder().addService(ServerInterceptors.intercept(rpc, SpeechRpc.authentication(security)))
                        .useBlockingTaskExecutor(true).build())
                .annotatedService(events).build();
    }

    @Override public void start() { server.start().join(); running = true; }
    @Override public void stop() { server.stop().join(); running = false; }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }
    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
}
