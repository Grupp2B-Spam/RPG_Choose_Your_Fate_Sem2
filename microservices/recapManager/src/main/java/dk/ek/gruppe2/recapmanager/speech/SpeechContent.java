package dk.ek.gruppe2.recapmanager.speech;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.SpeechRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared identity for queue and gRPC; changes in synthesis options invalidate the cache. */
public final class SpeechContent {
    private SpeechContent() {}

    public static SpeechRequest request(String text, SpeechProperties properties) {
        return SpeechRequest.newBuilder().setText(text).setVoiceId(properties.getVoiceId())
                .setModel(properties.getModel()).setOutputFormat(properties.getOutputFormat())
                .setContentHash(hash(text, properties.getVoiceId(), properties.getModel(), properties.getOutputFormat()))
                .setWaitMillis(properties.getWaitMillis()).build();
    }

    public static String hash(String text, String voice, String model, String outputFormat) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Each exact UTF-8 string has a four-byte unsigned big-endian byte-length prefix.
            for (String value : new String[]{"recap-speech-v1", text, voice, model, outputFormat}) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
