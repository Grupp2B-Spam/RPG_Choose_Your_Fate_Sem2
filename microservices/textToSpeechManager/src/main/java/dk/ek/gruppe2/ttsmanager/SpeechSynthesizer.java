package dk.ek.gruppe2.ttsmanager;

@FunctionalInterface
public interface SpeechSynthesizer {
    byte[] synthesize(SpeechSpec specification);
}
