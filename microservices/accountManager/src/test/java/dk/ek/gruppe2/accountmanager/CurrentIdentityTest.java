package dk.ek.gruppe2.accountmanager;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CurrentIdentityTest {
    private static final String ISSUER = "https://identity.example.test/realms/choose-your-fate";
    private static final String AUDIENCE = "account-manager";
    private static HttpServer jwksServer;
    private static RSAKey signingKey;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void startJwksServer(DynamicPropertyRegistry registry) throws IOException, JOSEException {
        signingKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        jwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] jwks = new JWKSet(signingKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwksServer.createContext("/certs", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jwks.length);
                exchange.getResponseBody().write(jwks);
            }
        });
        jwksServer.start();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences[0]", () -> AUDIENCE);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://127.0.0.1:" + jwksServer.getAddress().getPort() + "/certs");
    }

    @AfterAll
    static void stopJwksServer() {
        if (jwksServer != null) {
            jwksServer.stop(0);
        }
    }

    @Test
    void returnsIdentityFromAValidAccessToken() throws Exception {
        var response = getIdentity(token(ISSUER, AUDIENCE, Instant.now().plusSeconds(300), signingKey));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(ISSUER, "player-123", "test-player");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
    }

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        assertThat(getIdentity(null).statusCode()).isEqualTo(401);
    }

    @Test
    void rejectsWrongIssuerAudienceExpiryAndSignature() throws Exception {
        Instant validExpiry = Instant.now().plusSeconds(300);
        var untrustedKey = new RSAKeyGenerator(2048).keyID(signingKey.getKeyID()).generate();
        assertThat(getIdentity(token("https://wrong.example.test", AUDIENCE, validExpiry, signingKey))
                .statusCode()).isEqualTo(401);
        assertThat(getIdentity(token(ISSUER, "choose-your-fate-web", validExpiry, signingKey))
                .statusCode()).isEqualTo(401);
        assertThat(getIdentity(token(ISSUER, AUDIENCE, Instant.now().minusSeconds(120), signingKey))
                .statusCode()).isEqualTo(401);
        assertThat(getIdentity(token(ISSUER, AUDIENCE, validExpiry, untrustedKey)).statusCode()).isEqualTo(401);
    }

    private static String token(String issuer, String audience, Instant expiry, RSAKey key) throws JOSEException {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("player-123")
                .claim("preferred_username", "test-player").expirationTime(Date.from(expiry)).build();
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build();
        var token = new SignedJWT(header, claims);
        token.sign(new RSASSASigner(key));
        return token.serialize();
    }

    private HttpResponse<String> getIdentity(String token) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/accounts/me"))
                .timeout(Duration.ofSeconds(5));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
