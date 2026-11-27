package dk.ek.gruppe2.ttsmanager;

import java.util.concurrent.TimeUnit;

public final class SpeechLookup {
    public record Result(String status, String audioUrl) {}
    private final JobStore jobs;
    private final AudioStore audio;
    private final WorkPublisher publisher;

    public SpeechLookup(JobStore jobs, AudioStore audio, WorkPublisher publisher) {
        this.jobs = jobs; this.audio = audio; this.publisher = publisher;
    }

    public Result getOrCreate(SpeechSpec spec, long waitMillis) {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitMillis);
        var job = jobs.ensure(spec);
        if ("READY".equals(job.state())) return ready(spec);
        if ("FAILED".equals(job.state())) return new Result("FAILED", "");
        // QUEUED may be left by a crash before publication. Re-publishing is safe through the lease.
        if ("QUEUED".equals(job.state())) publisher.publish(spec);
        while (System.nanoTime() < until) {
            try { TimeUnit.NANOSECONDS.sleep(Math.min(TimeUnit.MILLISECONDS.toNanos(50), until - System.nanoTime())); }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt(); return new Result("PENDING", "");
            }
            job = jobs.get(spec.contentHash());
            if ("READY".equals(job.state())) return ready(spec);
            if ("FAILED".equals(job.state())) return new Result("FAILED", "");
        }
        return new Result("PENDING", "");
    }

    private Result ready(SpeechSpec spec) { return new Result("READY", audio.downloadUrl(SpeechProcessor.objectKey(spec))); }
}
