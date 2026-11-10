package dk.ek.gruppe2.recapmanager.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CharacterAuthorizationService {
    private final CharacterAccessChecker characterAccessChecker;

    public CharacterAuthorizationService(CharacterAccessChecker characterAccessChecker) {
        this.characterAccessChecker = characterAccessChecker;
    }

    public boolean canAccessCharacter(Integer characterId, Authentication authentication) {
        if (characterId == null || characterId <= 0
                || !(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            return false;
        }
        String subject = token.getToken().getSubject();
        return subject != null && !subject.isBlank() && characterAccessChecker.canAccess(characterId, subject);
    }

    /** Tool arguments are selected by the model, so check every tool invocation as well. */
    public void requireAccessToCharacter(Integer characterId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (characterId != null && characterId > 0
                && authentication instanceof JwtAuthenticationToken && authentication.isAuthenticated()) {
            boolean admin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
            boolean user = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER"));
            if (admin || (user && canAccessCharacter(characterId, authentication))) {
                return;
            }
        }
        throw new AccessDeniedException("Access to character denied");
    }
}
