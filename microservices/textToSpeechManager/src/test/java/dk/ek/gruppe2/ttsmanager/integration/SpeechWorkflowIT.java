package dk.ek.gruppe2.ttsmanager.integration;

import dk.ek.gruppe2.ttsmanager.*;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SpeechWorkflowIT {
    @Test void recapEventAndRpcTextUseTheSameTtsOwnedCacheIdentity() {
        var f = new WorkflowFixture();
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var event = json.readTree(f.eventJson());
        f.events.process(event);
        f.events.process(event);
        assertThat(f.lookup.getOrCreate(f.policy.specification(f.spec.text()), 0).status()).isEqualTo("READY");
        assertThat(f.jobs.get(f.spec.contentHash()).spec()).isEqualTo(f.spec);
        assertThat(f.providerCalls).hasValue(1);
        assertThat(f.publications).isEmpty();
    }

    @Test void olderRecapEventsCannotOverrideTheTtsConfiguration() {
        var f = new WorkflowFixture();
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        f.events.process(json.readTree("""
                {"eventId":"old-event","eventType":"RecapGenerated","eventVersion":1,
                 "text":"A journey","contentHash":"caller-chosen","voiceId":"caller-voice",
                 "model":"caller-model","outputFormat":"caller-format"}
                """));
        assertThat(f.jobs.get(f.spec.contentHash()).spec()).isEqualTo(f.spec);
        assertThat(f.audio.objects).containsKey(SpeechProcessor.objectKey(f.spec));
        assertThat(f.providerCalls).hasValue(1);
    }

    @Test void queuedJobKeepsItsStoredSettingsWhileNewRequestsUseTheUpdatedConfiguration() {
        var f = new WorkflowFixture();
        f.lookup.getOrCreate(f.spec, 0);
        var nextPolicy = new SpeechPolicy(1000, 5000, "new-voice", "new-model", "mp3_22050_32");
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        var events = new SpeechEvents(f.processor, nextPolicy, f.security, json, f.jdbc, f.jobs);
        events.process(json.readTree(json.writeValueAsString(java.util.Map.of(
                "eventId", "work-1", "eventType", "SpeechRequested", "eventVersion", 2, "contentHash", f.spec.contentHash()))));
        assertThat(f.jobs.get(f.spec.contentHash()).spec()).isEqualTo(f.spec);
        assertThat(f.audio.objects).containsKey(SpeechProcessor.objectKey(f.spec));

        events.process(json.readTree(f.eventJson()));
        var newSpec = nextPolicy.specification(f.spec.text());
        assertThat(newSpec.contentHash()).isNotEqualTo(f.spec.contentHash());
        assertThat(f.jobs.get(newSpec.contentHash()).state()).isEqualTo("READY");
        assertThat(f.audio.objects).hasSize(2).containsKey(SpeechProcessor.objectKey(newSpec));
        assertThat(f.providerCalls).hasValue(2);
    }

    @Test void duplicateEventsGenerateOnceAndRepeatedLookupsIssueFreshUrlsWithoutNewWork() {
        var f = new WorkflowFixture();
        f.processor.generate(f.spec);
        f.processor.generate(f.spec);
        var first = f.lookup.getOrCreate(f.spec, 0);
        var second = f.lookup.getOrCreate(f.spec, 0);
        assertThat(f.providerCalls).hasValue(1);
        assertThat(f.audio.objects).hasSize(1);
        assertThat(first.status()).isEqualTo("READY");
        assertThat(second.status()).isEqualTo("READY");
        assertThat(first.audioUrl()).isNotEqualTo(second.audioUrl());
        assertThat(f.publications).isEmpty();
        assertThat(f.jobs.get(f.spec.contentHash()).attempts()).isEqualTo(1);
    }

    @Test void rpcCacheMissPublishesDurableWorkAndReturnsPendingWithoutDoingPaidGeneration() {
        var f = new WorkflowFixture();
        assertThat(f.lookup.getOrCreate(f.spec, 0).status()).isEqualTo("PENDING");
        assertThat(f.publications).containsExactly(f.spec);
        assertThat(f.providerCalls).hasValue(0);
        assertThat(f.jobs.get(f.spec.contentHash()).state()).isEqualTo("QUEUED");
    }

    @Test void publicationFailureIsNotReportedAsAcceptedPendingWork() {
        var f = new WorkflowFixture();
        var lookup = new SpeechLookup(f.jobs, f.audio, spec -> { throw new IllegalStateException("Broker down"); });
        assertThatThrownBy(() -> lookup.getOrCreate(f.spec, 0)).isInstanceOf(IllegalStateException.class);
        assertThat(f.jobs.get(f.spec.contentHash()).state()).isEqualTo("QUEUED");
        // A later request can repair the interrupted handoff.
        assertThat(f.lookup.getOrCreate(f.spec, 0).status()).isEqualTo("PENDING");
        assertThat(f.publications).containsExactly(f.spec);
    }

    @Test void uploadedAudioSurvivesACrashBeforeTheMetadataCommit() {
        var f = new WorkflowFixture();
        f.jobs.ensure(f.spec);
        String oldLease = f.jobs.claim(f.spec.contentHash());
        f.audio.put(SpeechProcessor.objectKey(f.spec), new byte[]{4, 5});
        f.clock.advance(Duration.ofSeconds(121));
        f.processor.generate(f.spec);
        assertThat(f.providerCalls).hasValue(0);
        assertThat(f.jobs.get(f.spec.contentHash()).state()).isEqualTo("READY");
        assertThat(f.audio.objects.get(SpeechProcessor.objectKey(f.spec))).containsExactly(4, 5);
        assertThat(f.jobs.ready(f.spec.contentHash(), oldLease)).isFalse();
    }

    @Test void concurrentWorkersCannotBothAcquireTheSameGenerationLease() throws Exception {
        var f = new WorkflowFixture();
        f.jobs.ensure(f.spec);
        try (var threads = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var a = threads.submit(() -> { start.await(); return f.jobs.claim(f.spec.contentHash()); });
            var b = threads.submit(() -> { start.await(); return f.jobs.claim(f.spec.contentHash()); });
            start.countDown();
            assertThat(java.util.stream.Stream.of(a.get(2, TimeUnit.SECONDS), b.get(2, TimeUnit.SECONDS))
                    .filter(java.util.Objects::nonNull).count()).isEqualTo(1);
        }
        assertThat(f.jobs.get(f.spec.contentHash()).attempts()).isEqualTo(1);
    }

    @Test void repeatedProviderFailureStopsAtTheAttemptLimitAndCannotCacheErrorBytes() {
        var f = new WorkflowFixture();
        var processor = new SpeechProcessor(f.jobs, f.audio, spec -> {
            f.providerCalls.incrementAndGet(); throw new IllegalStateException("Provider down");
        });
        for (int i = 0; i < 3; i++) assertThatThrownBy(() -> processor.generate(f.spec)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> processor.generate(f.spec)).isInstanceOf(SpeechProcessor.Exhausted.class);
        assertThat(f.providerCalls).hasValue(3);
        assertThat(f.audio.objects).isEmpty();
        assertThat(f.lookup.getOrCreate(f.spec, 0).status()).isEqualTo("FAILED");
        assertThat(f.publications).isEmpty();
    }

    @Test void expiredFinalAttemptCannotLeaveTheJobPendingForever() {
        var f = new WorkflowFixture();
        f.jobs.ensure(f.spec);
        for (int i = 0; i < 3; i++) {
            assertThat(f.jobs.claim(f.spec.contentHash())).isNotNull();
            f.clock.advance(Duration.ofSeconds(121));
        }
        assertThat(f.lookup.getOrCreate(f.spec, 0).status()).isEqualTo("FAILED");
    }
}
