package dk.ek.gruppe2.chooseyourfate.availability.routing;

public class InvalidDatabaseStateTransitionException extends RuntimeException {

    public InvalidDatabaseStateTransitionException(String message) {
        super(message);
    }
}
