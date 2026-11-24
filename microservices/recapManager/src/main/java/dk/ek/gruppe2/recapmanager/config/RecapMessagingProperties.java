package dk.ek.gruppe2.recapmanager.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.recap-events")
public class RecapMessagingProperties {
    @NotBlank private String exchange = "choose-your-fate.recap";
    @NotBlank private String routingKey = "recap.generated.v1";
    @Min(1) @Max(60000) private long confirmTimeoutMillis = 3000;
    @Min(1000) private long retryDelayMillis = 10000;
}
