package dk.ek.gruppe2.accountmanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

@Component
class VerificationKey {
    private static final Pattern PUBLIC_KEY_PEM = Pattern.compile(
            "\\s*-----BEGIN PUBLIC KEY-----\\s+([A-Za-z0-9+/=\\s]+)-----END PUBLIC KEY-----\\s*");

    private final String issuer;
    private final String audience;
    private final String publicKeyBase64;

    VerificationKey(@Value("${app.jwt.public-key-file}") String keyFile,
                    @Value("${app.jwt.issuer}") String issuer,
                    @Value("${app.jwt.audience}") String audience) throws IOException, GeneralSecurityException {
        if (!URI.create(issuer).isAbsolute() || audience.isBlank()) {
            throw new IllegalArgumentException("Configure an absolute JWT issuer URI and a non-blank audience");
        }

        var pem = PUBLIC_KEY_PEM.matcher(Files.readString(Path.of(keyFile)));
        if (!pem.matches()) {
            throw new IllegalArgumentException("JWT public key file must contain a single X.509 PUBLIC KEY PEM");
        }
        byte[] encodedKey = Base64.getDecoder().decode(pem.group(1).replaceAll("\\s", ""));
        var key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedKey));
        if (key.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("JWT RSA public key must have at least 2048 bits");
        }

        this.issuer = issuer;
        this.audience = audience;
        this.publicKeyBase64 = Base64.getEncoder().encodeToString(key.getEncoded());
    }

    String issuer() {
        return issuer;
    }

    String audience() {
        return audience;
    }

    String publicKeyBase64() {
        return publicKeyBase64;
    }
}
