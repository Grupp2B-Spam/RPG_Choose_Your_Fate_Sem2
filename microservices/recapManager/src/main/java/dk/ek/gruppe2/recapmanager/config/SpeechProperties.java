package dk.ek.gruppe2.recapmanager.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.speech")
public class SpeechProperties {
    @Pattern(regexp = "mock|grpc") private String mode = "mock";
    @NotBlank private String target = "localhost:9093";
    private boolean plaintext;
    @NotBlank private String voiceId = "JBFqnCBsd6RMkjVDRZzb";
    @NotBlank private String model = "eleven_turbo_v2_5";
    @NotBlank private String outputFormat = "mp3_44100_128";
    @Min(1) @Max(60000) private long waitMillis = 5000;
    @Min(1) @Max(60000) private long deadlineGraceMillis = 1000;
    @Pattern(regexp = "READY|PENDING|FAILED") private String mockStatus = "PENDING";
    // A real fixture URL must be supplied to simulate playable READY audio.
    private String mockAudioUrl = "";
}
