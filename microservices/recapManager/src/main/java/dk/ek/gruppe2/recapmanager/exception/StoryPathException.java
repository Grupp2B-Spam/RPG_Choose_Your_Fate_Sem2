package dk.ek.gruppe2.recapmanager.exception;

public class StoryPathException extends RuntimeException {
    public StoryPathException(String message) { super(message); }
    public StoryPathException(String message, Throwable cause) { super(message, cause); }
}
