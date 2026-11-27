package dk.ek.gruppe2.accountmanager;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CurrentIdentityController {
    @GetMapping("/api/accounts/me")
    ResponseEntity<CurrentIdentity> currentIdentity(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CurrentIdentity(
                token.getIssuer().toString(), token.getSubject(), token.getClaimAsString("preferred_username")));
    }

    record CurrentIdentity(String issuer, String subject, String username) {}
}
