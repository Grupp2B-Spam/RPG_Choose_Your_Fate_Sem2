package dk.ek.gruppe2.recapmanager.speech;

import dk.ek.gruppe2.recapmanager.config.SpeechProperties;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.SpeechRequest;

/** Request narration without choosing the speech service's synthesis settings or cache identity. */
public final class SpeechContent {
    private SpeechContent() {}

    public static SpeechRequest request(String text, SpeechProperties properties) {
        return SpeechRequest.newBuilder().setText(text)
                .setWaitMillis(properties.getWaitMillis()).build();
    }
}
