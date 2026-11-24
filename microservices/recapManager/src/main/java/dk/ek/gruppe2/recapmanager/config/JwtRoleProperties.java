package dk.ek.gruppe2.recapmanager.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.security.jwt.roles")
public class JwtRoleProperties {
    @Pattern(regexp = "realm|client|both") private String source = "realm";
    private String clientId = "";
    @NotBlank private String userRole = "USER";
    @NotBlank private String adminRole = "ADMIN";

    @AssertTrue(message = "KEYCLOAK_ROLE_CLIENT_ID is required for client or both role sources")
    public boolean isClientConfigured() {
        return "realm".equals(source) || (clientId != null && !clientId.isBlank());
    }
}
