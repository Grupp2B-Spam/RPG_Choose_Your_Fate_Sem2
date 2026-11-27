package dk.ek.gruppe2.ttsmanager;

/** Generation runs within the event request; acknowledging it never abandons a background task. */
public final class SpeechProcessor {
    public static final class RetryLater extends RuntimeException {
        public RetryLater() { super("Speech work needs redelivery"); }
    }
    public static final class Exhausted extends RuntimeException {
        public Exhausted() { super("Speech generation attempts exhausted"); }
    }
    private final JobStore jobs;
    private final AudioStore audio;
    private final SpeechSynthesizer provider;

    public SpeechProcessor(JobStore jobs, AudioStore audio, SpeechSynthesizer provider) {
        this.jobs = jobs; this.audio = audio; this.provider = provider;
    }

    public void generate(SpeechSpec spec) {
        var job = jobs.ensure(spec);
        if ("READY".equals(job.state())) return;
        if ("FAILED".equals(job.state())) throw new Exhausted();
        String token = jobs.claim(spec.contentHash());
        if (token == null) throw new RetryLater();
        try {
            String key = objectKey(spec);
            // Recover a crash after upload without making another paid provider call.
            if (!audio.exists(key)) audio.put(key, provider.synthesize(spec));
            if (!jobs.ready(spec.contentHash(), token)) throw new RetryLater();
        } catch (RuntimeException failure) {
            jobs.failed(spec.contentHash(), token);
            if ("FAILED".equals(jobs.get(spec.contentHash()).state())) throw new Exhausted();
            throw new RetryLater();
        }
    }

    public static String objectKey(SpeechSpec spec) { return "speech/v1/" + spec.contentHash() + ".mp3"; }
}
