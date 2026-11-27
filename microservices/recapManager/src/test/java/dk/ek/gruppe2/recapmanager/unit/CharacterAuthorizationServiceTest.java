package dk.ek.gruppe2.recapmanager.unit;

import dk.ek.gruppe2.recapmanager.security.*;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CharacterAuthorizationServiceTest {
    private final CharacterAccessChecker ownership = mock(CharacterAccessChecker.class);
    private final CharacterAuthorizationService service = new CharacterAuthorizationService(ownership);

    @AfterEach void clearCaller() { SecurityContextHolder.clearContext(); }

    @Test void ownerMayRequestTheirCharacter() {
        authenticate("player-1", "ROLE_USER");
        when(ownership.canAccess(1, "player-1")).thenReturn(true);
        assertThatCode(() -> service.requireAccessToCharacter(1)).doesNotThrowAnyException();
    }

    @Test void anotherPlayersCharacterIsDenied() {
        authenticate("player-2", "ROLE_USER");
        assertThatThrownBy(() -> service.requireAccessToCharacter(1)).isInstanceOf(AccessDeniedException.class);
        verify(ownership).canAccess(1, "player-2");
    }

    @Test void adminCanRequestACharacterWithoutAnOwnershipLookup() {
        authenticate("administrator", "ROLE_ADMIN");
        assertThatCode(() -> service.requireAccessToCharacter(1)).doesNotThrowAnyException();
        verifyNoInteractions(ownership);
    }

    @Test void aCallerWithoutAnApplicationRoleCannotReadRecaps() {
        authenticate("player-1", "ROLE_OTHER");
        assertThatThrownBy(() -> service.requireAccessToCharacter(1)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(ownership);
    }

    private void authenticate(String subject, String role) {
        var jwt = Jwt.withTokenValue("test-caller").header("alg", "RS256").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority(role))));
    }
}
