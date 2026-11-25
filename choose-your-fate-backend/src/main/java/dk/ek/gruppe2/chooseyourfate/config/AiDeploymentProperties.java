package dk.ek.gruppe2.chooseyourfate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiDeploymentProperties {

    private boolean enabled = true;
    private boolean stripMarkdown = true;
}
