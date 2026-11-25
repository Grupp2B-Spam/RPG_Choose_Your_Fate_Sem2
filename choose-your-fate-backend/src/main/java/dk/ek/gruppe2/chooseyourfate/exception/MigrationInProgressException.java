package dk.ek.gruppe2.chooseyourfate.exception;

public class MigrationInProgressException extends RuntimeException {
    public MigrationInProgressException(String message) {
        super(message);
    }
}
