package dk.ek.gruppe2.recapmanager.config;

import dk.ek.gruppe2.recapmanager.security.CharacterAccessChecker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(MockCharacterAccessProperties.class)
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
    public JwtDecoder jwtDecoder(
            @Value("${app.security.jwt.public-key-location}") Resource publicKey,
            OAuth2TokenValidator<Jwt> recapJwtValidator) throws IOException {
        // Reads an X.509/SPKI PEM public key. The private key stays with AccountManager.
        try (var stream = publicKey.getInputStream()) {
            var decoder = NimbusJwtDecoder.withPublicKey(RsaKeyConverters.x509().convert(stream))
                    .signatureAlgorithm(SignatureAlgorithm.RS256).build();
            decoder.setJwtValidator(recapJwtValidator);
            return decoder;
        }
    }

    @Bean
    public OAuth2TokenValidator<Jwt> recapJwtValidator(
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.audience}") String audience) {
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
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Contract: a single role claim, exactly ROLE_USER or ROLE_ADMIN.
            Object role = jwt.getClaims().get("role");
            if ("ROLE_ADMIN".equals(role) || "ROLE_USER".equals(role)) {
                return List.of(new SimpleGrantedAuthority((String) role));
            }
            return List.of();
        });
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(CharacterAccessChecker.class)
    public CharacterAccessChecker characterAccessChecker(MockCharacterAccessProperties properties) {
        // No remote calls. An unknown character or missing ownership adapter always denies access.
        return (characterId, subject) -> properties.isMockCharacterAccess()
                && subject.equals(properties.getMockCharacterOwners().get(characterId));
    }
}
