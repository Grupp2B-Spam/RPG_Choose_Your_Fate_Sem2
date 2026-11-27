package dk.ek.gruppe2.ttsmanager.unit;

import dk.ek.gruppe2.ttsmanager.JwtSecurity;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.*;

class JwtSecurityTest {
    private final JwtDecoder verifiedLocalPlayer = token -> Jwt.withTokenValue(token)
            .header("alg", "RS256").subject("player-1").claim("role", "ROLE_USER").build();

    @Test void offlinePlayerRoleIsAcceptedOnlyByTheExplicitLocalPolicy() {
        var local = new JwtSecurity(verifiedLocalPlayer, verifiedLocalPlayer,
                Set.of("local-event-delivery"), "", "USER", "ADMIN", true);
        var production = new JwtSecurity(verifiedLocalPlayer, verifiedLocalPlayer,
                Set.of("local-event-delivery"), "", "USER", "ADMIN");

        assertThatCode(() -> local.requireCaller("Bearer player-token")).doesNotThrowAnyException();
        assertThatThrownBy(() -> production.requireCaller("Bearer player-token"))
                .isInstanceOf(JwtSecurity.Rejected.class);
    }

    @Test void localPlayerPermissionDoesNotGrantEventDeliveryPermission() {
        var local = new JwtSecurity(verifiedLocalPlayer, verifiedLocalPlayer,
                Set.of("local-event-delivery"), "", "USER", "ADMIN", true);

        assertThatThrownBy(() -> local.requireEventSender("Bearer player-token"))
                .isInstanceOf(JwtSecurity.Rejected.class);
    }
}
