package dk.ek.gruppe2.ttsmanager;

/** Persistent object storage; URLs are issued when requested rather than stored as expiring cache entries. */
public interface AudioStore {
    boolean exists(String key);
    void put(String key, byte[] audio);
    String downloadUrl(String key);
}
