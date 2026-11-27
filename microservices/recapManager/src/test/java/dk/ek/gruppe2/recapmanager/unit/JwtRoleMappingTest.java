package dk.ek.gruppe2.recapmanager.unit;

import dk.ek.gruppe2.recapmanager.config.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.*;

class JwtRoleMappingTest {
    private final SecurityConfig config = new SecurityConfig();

    @Test void onlyConfiguredClientCanGrantClientRoles() {
        var roles = new JwtRoleProperties();
        roles.setSource("client");
        roles.setClientId("recap-api");
        var jwt = token().claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .claim("resource_access", Map.of("recap-api", Map.of("roles", List.of("USER")),
                        "other-api", Map.of("roles", List.of("ADMIN")))).build();
        assertThat(config.jwtAuthenticationConverter(roles, "keycloak").convert(jwt).getAuthorities())
                .filteredOn(authority -> authority.getAuthority().startsWith("ROLE_"))
                .extracting("authority").containsExactly("ROLE_USER");
    }

    @Test void bothSourcesAndCustomRoleNamesAreSupported() {
        var roles = new JwtRoleProperties();
        roles.setSource("both");
        roles.setClientId("recap-api");
        roles.setUserRole("player");
        roles.setAdminRole("recap-administrator");
        var jwt = token().claim("realm_access", Map.of("roles", List.of("player")))
                .claim("resource_access", Map.of("recap-api", Map.of("roles", List.of("recap-administrator")))).build();
        assertThat(config.jwtAuthenticationConverter(roles, "keycloak").convert(jwt).getAuthorities())
                .filteredOn(authority -> authority.getAuthority().startsWith("ROLE_"))
                .extracting("authority").containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test void malformedRoleClaimsGrantNoPermissions() {
        var jwt = token().claim("realm_access", Map.of("roles", "ADMIN"))
                .claim("resource_access", "ADMIN").build();
        assertThat(config.jwtAuthenticationConverter(new JwtRoleProperties(), "keycloak")
                .convert(jwt).getAuthorities()).noneMatch(authority -> authority.getAuthority().startsWith("ROLE_"));
    }

    @Test void offlineModeRetainsTheLocalTokenHelperContract() {
        var jwt = token().claim("role", "ROLE_USER").build();
        assertThat(config.jwtAuthenticationConverter(new JwtRoleProperties(), "pem").convert(jwt).getAuthorities())
                .filteredOn(authority -> authority.getAuthority().startsWith("ROLE_"))
                .extracting("authority").containsExactly("ROLE_USER");
    }

    private Jwt.Builder token() { return Jwt.withTokenValue("example").header("alg", "RS256").subject("player-1"); }
}
