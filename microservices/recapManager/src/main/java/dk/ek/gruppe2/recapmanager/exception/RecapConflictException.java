package dk.ek.gruppe2.recapmanager.exception;

public class RecapConflictException extends RuntimeException {
    public RecapConflictException() {
        super("This summary was updated by another request. Retry using the latest saved summary.");
    }
}
