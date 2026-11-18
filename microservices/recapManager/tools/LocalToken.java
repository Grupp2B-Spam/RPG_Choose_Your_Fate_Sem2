import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/** Development-only issuer. Run from recapManager: java tools/LocalToken.java [subject] [role] */
class LocalToken {
    public static void main(String[] args) throws Exception {
        String subject = args.length > 0 ? args[0] : "player-1";
        String role = args.length > 1 ? args[1] : "ROLE_USER";
        if (!subject.matches("[a-zA-Z0-9_-]+") || !role.matches("ROLE_(USER|ADMIN)")) {
            throw new IllegalArgumentException("Subject must use letters, numbers, _ or -; role must be ROLE_USER or ROLE_ADMIN");
        }
        Path directory = Path.of(".local");
        Files.createDirectories(directory);
        Path privateFile = directory.resolve("recap-jwt-private.pem");
        Path publicFile = directory.resolve("recap-jwt-public.pem");
        if (!Files.exists(privateFile) && !Files.exists(publicFile)) {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keys = generator.generateKeyPair();
            Files.writeString(privateFile, pem("PRIVATE KEY", keys.getPrivate().getEncoded()));
            Files.writeString(publicFile, pem("PUBLIC KEY", keys.getPublic().getEncoded()));
        }
        if (!Files.isRegularFile(privateFile) || !Files.isRegularFile(publicFile)) {
            throw new IllegalStateException("Both .local/recap-jwt-private.pem and recap-jwt-public.pem must exist");
        }
        String encodedKey = Files.readString(privateFile).replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        var privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey)));
        long now = Instant.now().getEpochSecond();
        String claims = "{\"iss\":\"http://localhost:8092\",\"aud\":[\"choose-your-fate\"],\"sub\":\""
                + subject + "\",\"role\":\"" + role + "\",\"iat\":" + now + ",\"exp\":" + (now + 900) + "}";
        String unsigned = encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8))
                + "." + encode(claims.getBytes(StandardCharsets.UTF_8));
        var signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        System.out.println(unsigned + "." + encode(signature.sign()));
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String pem(String label, byte[] bytes) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(bytes)
                + "\n-----END " + label + "-----\n";
    }
}
