package dk.ek.gruppe2.ttsmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import tools.jackson.databind.ObjectMapper;

/** Bounded provider call. No user JWT is forwarded to ElevenLabs. */
public final class ElevenLabsSynthesizer implements SpeechSynthesizer {
    private final HttpClient http;
    private final URI baseUrl;
    private final String apiKey;
    private final Duration timeout;
    private final int maxAudioBytes;
    private final ObjectMapper json;

    public ElevenLabsSynthesizer(HttpClient http, URI baseUrl, String apiKey, Duration timeout,
                                  int maxAudioBytes, ObjectMapper json) {
        this.http = http;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.maxAudioBytes = maxAudioBytes;
        this.json = json;
    }

    @Override public byte[] synthesize(SpeechSpec spec) {
        String path = "/v1/text-to-speech/" + encode(spec.voiceId()) + "?output_format=" + encode(spec.outputFormat());
        var request = HttpRequest.newBuilder(baseUrl.resolve(path)).timeout(timeout)
                .header("xi-api-key", apiKey).header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(
                        Map.of("text", spec.text(), "model_id", spec.model())))).build();
        var pending = http.sendAsync(request, info -> new LimitedBody(maxAudioBytes));
        try {
            // Includes the entire response body, not just receipt of headers.
            var response = pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Speech provider returned HTTP " + response.statusCode());
            }
            if (response.body().length == 0) throw new IllegalStateException("Speech provider returned empty audio");
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Speech generation interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Speech provider unavailable", exception);
        } finally { pending.cancel(true); }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private LimitedBody(int limit) { this.limit = limit; }
        @Override public CompletionStage<byte[]> getBody() { return body; }
        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - bytes.size()) {
                    subscription.cancel();
                    body.completeExceptionally(new IOException("Speech provider audio exceeds configured limit"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error) { body.completeExceptionally(error); }
        @Override public void onComplete() { body.complete(bytes.toByteArray()); }
    }
}
