package dk.ek.gruppe2.ttsmanager;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;

public final class MinioAudioStore implements AudioStore {
    private final MinioClient storage;
    private final MinioClient signer;
    private final String bucket;
    private final String region;
    private final int urlLifetime;

    public MinioAudioStore(MinioClient storage, MinioClient signer, String bucket, String region, int urlLifetime) {
        this.storage = storage; this.signer = signer; this.bucket = bucket;
        this.region = region; this.urlLifetime = urlLifetime;
    }

    /** Local startup only; preserve existing buckets and their access policies. */
    public void initializeBucket() {
        try {
            if (!storage.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                storage.makeBucket(MakeBucketArgs.builder().bucket(bucket).region(region).build());
            }
        } catch (ErrorResponseException error) {
            // A second local instance may create the same owned bucket between HEAD and PUT.
            if (!"BucketAlreadyOwnedByYou".equals(error.errorResponse().code())) {
                throw new IllegalStateException("Audio bucket initialization failed", error);
            }
        } catch (Exception error) { throw new IllegalStateException("Audio bucket initialization failed", error); }
    }

    @Override public boolean exists(String key) {
        try { storage.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build()); return true; }
        catch (ErrorResponseException error) {
            if ("NoSuchKey".equals(error.errorResponse().code()) || "NoSuchObject".equals(error.errorResponse().code())) return false;
            throw new IllegalStateException("Audio storage lookup failed", error);
        } catch (Exception error) { throw new IllegalStateException("Audio storage unavailable", error); }
    }

    @Override public void put(String key, byte[] audio) {
        try {
            storage.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .contentType("audio/mpeg").stream(new ByteArrayInputStream(audio), audio.length, -1).build());
        } catch (Exception error) { throw new IllegalStateException("Audio upload failed", error); }
    }

    @Override public String downloadUrl(String key) {
        try {
            // Sign against the browser-reachable endpoint, with explicit region to avoid a lookup.
            return signer.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(bucket).object(key).region(region).expiry(urlLifetime).build());
        } catch (Exception error) { throw new IllegalStateException("Audio URL signing failed", error); }
    }
}
