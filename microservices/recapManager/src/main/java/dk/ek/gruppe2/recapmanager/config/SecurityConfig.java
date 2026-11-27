package dk.ek.gruppe2.recapmanager.config;

import dk.ek.gruppe2.recapmanager.security.CharacterAccessChecker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({MockCharacterAccessProperties.class, JwtRoleProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            @Value("${app.security.public-docs:false}") boolean publicDocs) throws Exception {
        return http
                // Credentials are sent explicitly as Bearer headers, never session cookies.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll();
                    if (publicDocs) {
                        authorize.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
                                .permitAll();
                    }
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.jwt.mode", havingValue = "pem")
    public JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.public-key-location}") Resource publicKey,
            OAuth2TokenValidator<Jwt> recapJwtValidator) throws IOException {
        // Explicit offline development mode; never accepts a private key or signs tokens.
        try (var stream = publicKey.getInputStream()) {
            var decoder = NimbusJwtDecoder.withPublicKey(RsaKeyConverters.x509().convert(stream))
                    .signatureAlgorithm(SignatureAlgorithm.RS256).build();
            decoder.setJwtValidator(recapJwtValidator);
            return decoder;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.jwt.mode", havingValue = "keycloak", matchIfMissing = true)
    public JwtDecoder keycloakJwtDecoder(
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.jwk-set-uri:}") String jwkSetUri,
            @Value("${app.security.jwt.jwk-timeout-millis:3000}") int timeoutMillis,
            OAuth2TokenValidator<Jwt> recapJwtValidator) {
        Assert.hasText(issuer, "KEYCLOAK_ISSUER_URI must be configured");
        String keys = jwkSetUri.isBlank()
                ? issuer.replaceAll("/+$", "") + "/protocol/openid-connect/certs" : jwkSetUri;
        URI uri = URI.create(keys);
        Assert.isTrue(uri.getHost() != null && ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())),
                "Keycloak JWK Set URI must be an absolute HTTP(S) URL");
        Assert.isTrue(timeoutMillis > 0 && timeoutMillis <= 60000, "JWK timeout must be between 1 and 60000 ms");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMillis));
        factory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        // Use only the configured trusted key endpoint, never an iss/jku URL supplied by a token.
        // Key retrieval is lazy and cached by Nimbus; startup does not call Keycloak.
        var decoder = NimbusJwtDecoder.withJwkSetUri(keys).jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(factory)).build();
        OAuth2TokenValidator<Jwt> accessToken = jwt -> "Bearer".equals(jwt.getClaimAsString("typ"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "A Keycloak access token is required", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(recapJwtValidator, accessToken));
        return decoder;
    }

    @Bean
    public OAuth2TokenValidator<Jwt> recapJwtValidator(
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience) {
        Assert.hasText(issuer, "KEYCLOAK_ISSUER_URI must be configured (or use the local profile)");
        Assert.hasText(audience, "KEYCLOAK_AUDIENCE must be configured (or use the local profile)");
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()
                    || jwt.getAudience() == null || !jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                        "Token requires exp, sub and the expected audience", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        // Standard validators enforce issuer, expiry and not-before (60 seconds of clock skew).
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), requiredClaims);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(JwtRoleProperties properties,
            @Value("${app.security.jwt.mode:keycloak}") String mode) {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            if ("pem".equals(mode)) {
                Object role = jwt.getClaims().get("role");
                return "ROLE_ADMIN".equals(role) || "ROLE_USER".equals(role)
                        ? List.of(new SimpleGrantedAuthority((String) role)) : List.of();
            }
            Set<String> roles = new LinkedHashSet<>();
            if (!"client".equals(properties.getSource())) addRoles(roles, jwt.getClaims().get("realm_access"));
            if (!"realm".equals(properties.getSource())
                    && jwt.getClaims().get("resource_access") instanceof Map<?, ?> clients) {
                addRoles(roles, clients.get(properties.getClientId()));
            }
            Set<GrantedAuthority> authorities = new LinkedHashSet<>();
            if (roles.contains(properties.getUserRole())) authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if (roles.contains(properties.getAdminRole())) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            return authorities;
        });
        return converter;
    }

    private void addRoles(Set<String> target, Object claim) {
        if (claim instanceof Map<?, ?> access && access.get("roles") instanceof Collection<?> roles) {
            roles.stream().filter(String.class::isInstance).map(String.class::cast).forEach(target::add);
        }
    }

    @Bean
    @ConditionalOnMissingBean(CharacterAccessChecker.class)
    public CharacterAccessChecker characterAccessChecker(MockCharacterAccessProperties properties) {
        // No remote calls. An unknown character or missing ownership adapter always denies access.
        return (characterId, subject) -> properties.isMockCharacterAccess()
                && subject.equals(properties.getMockCharacterOwners().get(characterId));
    }
}
