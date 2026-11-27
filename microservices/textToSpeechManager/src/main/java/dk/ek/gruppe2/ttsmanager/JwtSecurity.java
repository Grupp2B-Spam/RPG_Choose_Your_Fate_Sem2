package dk.ek.gruppe2.ttsmanager;

import java.util.*;
import org.springframework.security.oauth2.jwt.*;

public final class JwtSecurity {
    public static final class Rejected extends RuntimeException {
        private final boolean authenticated;
        public Rejected(boolean authenticated) { super("Request identity rejected"); this.authenticated = authenticated; }
        public boolean authenticated() { return authenticated; }
    }
    private final JwtDecoder callers;
    private final JwtDecoder events;
    private final Set<String> eventSubjects;
    private final String clientId;
    private final Set<String> allowedRoles;
    private final boolean localRoles;

    public JwtSecurity(JwtDecoder callers, JwtDecoder events, Set<String> eventSubjects,
                       String clientId, String userRole, String adminRole) {
        this(callers, events, eventSubjects, clientId, userRole, adminRole, false);
    }

    public JwtSecurity(JwtDecoder callers, JwtDecoder events, Set<String> eventSubjects,
                       String clientId, String userRole, String adminRole, boolean localRoles) {
        this.callers = callers; this.events = events; this.eventSubjects = Set.copyOf(eventSubjects);
        this.clientId = clientId; this.allowedRoles = Set.of(userRole, adminRole);
        this.localRoles = localRoles;
    }

    public void requireCaller(String authorization) {
        Jwt jwt = decode(callers, authorization);
        Set<String> roles = new HashSet<>();
        if (localRoles) {
            allowedRoles.stream().filter(role -> ("ROLE_" + role).equals(jwt.getClaimAsString("role"))).forEach(roles::add);
        }
        addRoles(roles, jwt.getClaim("realm_access"));
        if (!clientId.isBlank() && jwt.getClaim("resource_access") instanceof Map<?, ?> clients) {
            addRoles(roles, clients.get(clientId));
        }
        if (Collections.disjoint(roles, allowedRoles)) throw new Rejected(true);
    }

    public void requireEventSender(String authorization) {
        if (!eventSubjects.contains(decode(events, authorization).getSubject())) throw new Rejected(true);
    }

    private Jwt decode(JwtDecoder decoder, String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) throw new Rejected(false);
        try { return decoder.decode(authorization.substring(7)); }
        catch (JwtException failure) { throw new Rejected(false); }
    }

    private void addRoles(Set<String> roles, Object access) {
        if (access instanceof Map<?, ?> values && values.get("roles") instanceof Collection<?> list) {
            list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(roles::add);
        }
    }
}
