package dk.ek.gruppe2.ttsmanager;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Internal TTS identity: exact text plus the service-owned synthesis settings. */
public record SpeechSpec(String contentHash, String text, String voiceId, String model, String outputFormat) {
    public static String hash(String text, String voice, String model, String format) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{"recap-speech-v1", text, voice, model, format}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

}
