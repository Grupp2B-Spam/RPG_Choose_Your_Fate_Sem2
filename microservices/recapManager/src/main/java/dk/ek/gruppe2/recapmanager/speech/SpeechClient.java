package dk.ek.gruppe2.recapmanager.speech;

import dk.ek.gruppe2.recapmanager.grpc.speech.v1.SpeechRequest;
import dk.ek.gruppe2.recapmanager.grpc.speech.v1.SpeechResponse;

@FunctionalInterface
public interface SpeechClient {
    SpeechResponse getOrCreate(SpeechRequest request);
}
