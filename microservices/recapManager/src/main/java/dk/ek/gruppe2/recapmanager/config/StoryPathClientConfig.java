package dk.ek.gruppe2.recapmanager.config;

import dk.ek.gruppe2.recapmanager.grpc.storypath.v1.StoryPathServiceGrpc;
import dk.ek.gruppe2.recapmanager.storypath.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
@EnableConfigurationProperties(StoryPathProperties.class)
public class StoryPathClientConfig {
    @Bean(destroyMethod = "shutdownNow")
    @ConditionalOnProperty(name = "app.storypath.mode", havingValue = "grpc", matchIfMissing = true)
    public ManagedChannel storyPathChannel(StoryPathProperties properties) {
        var builder = ManagedChannelBuilder.forTarget(properties.getTarget());
        if (properties.isPlaintext()) builder.usePlaintext();
        else builder.useTransportSecurity();
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.storypath.mode", havingValue = "grpc", matchIfMissing = true)
    public JourneyHistoryClient grpcJourneyHistoryClient(@Qualifier("storyPathChannel") ManagedChannel storyPathChannel, StoryPathProperties properties) {
        return new GrpcJourneyHistoryClient(StoryPathServiceGrpc.newBlockingStub(storyPathChannel), properties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storypath.mode", havingValue = "mock")
    public JourneyHistoryClient mockJourneyHistoryClient(StoryPathProperties properties) {
        return (characterId, cursor) -> properties.getMockChoices().stream()
                .filter(choice -> choice.getCharacterId() == characterId && choice.getSequence() > cursor)
                .sorted(Comparator.comparingLong(StoryPathProperties.MockChoice::getSequence))
                .map(choice -> new JourneyChoice(choice.getSequence(), choice.getChoiceId(), choice.getFromScene(),
                        choice.getToScene(), choice.getDescription(), choice.getConsequence(), choice.getStoryWeight()))
                .toList();
    }
}
