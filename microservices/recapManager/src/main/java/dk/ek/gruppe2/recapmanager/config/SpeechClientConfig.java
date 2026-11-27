package dk.ek.gruppe2.recapmanager.config;

import dk.ek.gruppe2.recapmanager.grpc.speech.v1.*;
import dk.ek.gruppe2.recapmanager.speech.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SpeechProperties.class)
public class SpeechClientConfig {
    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnProperty(name = "app.speech.mode", havingValue = "grpc")
    public ManagedChannel speechChannel(SpeechProperties properties) {
        var builder = ManagedChannelBuilder.forTarget(properties.getTarget());
        if (properties.isPlaintext()) builder.usePlaintext();
        else builder.useTransportSecurity();
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.speech.mode", havingValue = "grpc")
    public SpeechClient grpcSpeechClient(@Qualifier("speechChannel") ManagedChannel channel,
                                         SpeechProperties properties) {
        return new GrpcSpeechClient(SpeechServiceGrpc.newBlockingStub(channel), properties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.speech.mode", havingValue = "mock", matchIfMissing = true)
    public SpeechClient mockSpeechClient(SpeechProperties properties) {
        return request -> {
            var status = SpeechStatus.valueOf(properties.getMockStatus());
            if (status == SpeechStatus.READY && properties.getMockAudioUrl().isBlank()) {
                throw new IllegalStateException("READY speech fixture needs app.speech.mock-audio-url");
            }
            // Opaque identity supplied by this substitute for the remote service, not a local synthesis calculation.
            return SpeechResponse.newBuilder().setContentHash("0".repeat(64)).setStatus(status)
                    .setAudioUrl(status == SpeechStatus.READY ? properties.getMockAudioUrl() : "").build();
        };
    }
}
