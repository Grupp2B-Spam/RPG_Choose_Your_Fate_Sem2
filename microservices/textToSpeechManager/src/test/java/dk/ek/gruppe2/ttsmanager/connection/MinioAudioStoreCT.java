package dk.ek.gruppe2.ttsmanager.connection;

import com.sun.net.httpserver.HttpServer;
import dk.ek.gruppe2.ttsmanager.MinioAudioStore;
import io.minio.MinioClient;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

/** Real SDK/HTTP connection with a local storage substitute; no Docker dependency. */
class MinioAudioStoreCT {
    private HttpServer server;
    private OkHttpClient http;
    private MinioAudioStore audio;
    private final List<String> operations = new CopyOnWriteArrayList<>();
    private volatile boolean bucketExists;
    private String createFailure;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tts-audio", exchange -> {
            try {
                String method = exchange.getRequestMethod();
                operations.add(method + " " + exchange.getRequestURI());
                exchange.getRequestBody().readAllBytes();
                if ("HEAD".equals(method)) {
                    exchange.sendResponseHeaders(bucketExists ? 200 : 404, -1);
                } else if ("PUT".equals(method) && createFailure == null) {
                    bucketExists = true;
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    byte[] error = ("<Error><Code>" + createFailure + "</Code><Message>Test storage response</Message>"
                            + "<BucketName>tts-audio</BucketName><Resource>/tts-audio</Resource>"
                            + "<RequestId>test-request</RequestId><HostId>test-host</HostId></Error>").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/xml");
                    exchange.sendResponseHeaders("AccessDenied".equals(createFailure) ? 403 : 409, error.length);
                    exchange.getResponseBody().write(error);
                }
            } finally { exchange.close(); }
        });
        server.start();
        http = new OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).build();
        var client = MinioClient.builder().endpoint("http://127.0.0.1:" + server.getAddress().getPort())
                .credentials("test-access", "test-secret").region("us-east-1").httpClient(http).build();
        audio = new MinioAudioStore(client, client, "tts-audio", "us-east-1", 900);
    }

    @AfterEach void stop() {
        server.stop(0);
        http.connectionPool().evictAll();
        http.dispatcher().executorService().shutdownNow();
    }

    @Test void missingBucketIsCreatedOnceAndNextStartupLeavesItUntouched() {
        audio.initializeBucket();
        audio.initializeBucket();
        assertThat(bucketExists).isTrue();
        assertThat(operations).containsExactly("HEAD /tts-audio", "PUT /tts-audio", "HEAD /tts-audio");
    }

    @Test void existingBucketIsNotRecreatedOrGivenANewAccessPolicy() {
        bucketExists = true;
        audio.initializeBucket();
        assertThat(operations).containsExactly("HEAD /tts-audio");
    }

    @Test void anotherInstanceCreatingTheSameOwnedBucketDoesNotFailStartup() {
        createFailure = "BucketAlreadyOwnedByYou";
        assertThatCode(audio::initializeBucket).doesNotThrowAnyException();
        assertThat(operations).containsExactly("HEAD /tts-audio", "PUT /tts-audio");
    }

    @Test void deniedCreationIsReportedInsteadOfStartingWithUnusableStorage() {
        createFailure = "AccessDenied";
        assertThatThrownBy(audio::initializeBucket).isInstanceOf(IllegalStateException.class)
                .hasMessage("Audio bucket initialization failed");
        assertThat(operations).containsExactly("HEAD /tts-audio", "PUT /tts-audio");
    }
}
