package dk.ek.gruppe2.accountmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.issuer=https://accounts.example.test", "app.jwt.audience=game-test"
})
class VerificationKeyContractTest {
    @TempDir
    static Path keyDirectory;
    private static String publicKeyBase64;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void provisionPublicKey(DynamicPropertyRegistry registry) throws GeneralSecurityException, IOException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        var keyFile = Files.writeString(keyDirectory.resolve("public.pem"),
                "-----BEGIN PUBLIC KEY-----\n" + publicKeyBase64 + "\n-----END PUBLIC KEY-----\n");
        registry.add("app.jwt.public-key-file", keyFile::toString);
    }

    @Test
    void servesWsdlAtVersionedUrl() throws IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create(endpoint() + "/account-v1.wsdl"))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("urn:choose-your-fate:account:v1", "GetVerificationKey",
                    "location=\"" + endpoint() + "\"");
        }
    }

    @Test
    void returnsConfiguredMetadataAndPublicKey() throws IOException, InterruptedException {
        var response = getVerificationKey("");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("GetVerificationKeyResponse", "https://accounts.example.test",
                "game-test", "RS256", publicKeyBase64);
    }

    @Test
    void rejectsUnexpectedRequestFieldsWithSoapFault() throws IOException, InterruptedException {
        var response = getVerificationKey("<sec:unexpected/>");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("Fault").doesNotContain(publicKeyBase64);
    }

    @Test
    void rejectsPrivateKeyPem(@TempDir Path directory) throws GeneralSecurityException, IOException {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var privateKeyBase64 = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        var keyFile = Files.writeString(directory.resolve("private.pem"),
                "-----BEGIN PRIVATE KEY-----\n" + privateKeyBase64 + "\n-----END PRIVATE KEY-----\n");

        assertThatThrownBy(() -> new VerificationKey(keyFile.toString(), "https://accounts.example.test", "game-test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("X.509 PUBLIC KEY");
    }

    private HttpResponse<String> getVerificationKey(String fields) throws IOException, InterruptedException {
        String envelope = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                               xmlns:sec="urn:choose-your-fate:account:v1">
                  <soap:Body>
                    <sec:GetVerificationKeyRequest>%s</sec:GetVerificationKeyRequest>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(fields);
        var request = HttpRequest.newBuilder(URI.create(endpoint()))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(envelope)).build();
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private String endpoint() {
        return "http://127.0.0.1:" + port + "/ws";
    }
}
