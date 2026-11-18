package dk.ek.gruppe2.recapmanager.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.storypath")
public class StoryPathProperties {
    @Pattern(regexp = "grpc|mock") private String mode = "grpc";
    @NotBlank private String target = "localhost:9091";
    private boolean plaintext = false;
    @Min(1) @Max(60000) private long deadlineMillis = 3000;
    @Min(1) @Max(1000) private int pageSize = 100;
    @Min(1) @Max(1000) private int maxPages = 100;
    @Min(1) @Max(1000) private int generationBatchSize = 100;
    @Valid private List<MockChoice> mockChoices = new ArrayList<>();

    @Getter
    @Setter
    public static class MockChoice {
        @Min(1) private int characterId;
        @Min(1) private long sequence;
        @Min(1) private int choiceId;
        private String fromScene = "";
        private String toScene = "";
        @NotBlank private String description;
        private String consequence = "";
        private int storyWeight;
    }
}
