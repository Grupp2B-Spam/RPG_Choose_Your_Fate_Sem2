package dk.ek.gruppe2.ttsmanager;

public record SpeechPolicy(int maxCharacters, long maxWaitMillis, String voice, String model, String format) {
    public SpeechSpec specification(String text) {
        if (text == null || text.isBlank() || text.length() > maxCharacters) {
            throw new IllegalArgumentException("Invalid speech text");
        }
        return new SpeechSpec(SpeechSpec.hash(text, voice, model, format), text, voice, model, format);
    }
    public void validateWait(long waitMillis) {
        if (waitMillis < 0 || waitMillis > maxWaitMillis) throw new IllegalArgumentException("Invalid speech wait limit");
    }
}
