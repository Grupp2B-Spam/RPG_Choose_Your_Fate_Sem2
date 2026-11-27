package dk.ek.gruppe2.storycontent.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns the two kinds of refusal this service makes into HTTP responses. */
@RestControllerAdvice
public class ApiExceptions {

    /** The referenced content does not exist. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    /** The request is well-formed but would produce content that is not. */
    public static class InvalidContentException extends RuntimeException {
        public InvalidContentException(String message) {
            super(message);
        }
    }

    public record ErrorResponse(Instant timestamp, int status, String error, List<String> details) {
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> onNotFound(NotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(InvalidContentException.class)
    public ResponseEntity<ErrorResponse> onInvalidContent(InvalidContentException e) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> onBadRequest(MethodArgumentNotValidException e) {
        List<String> details = new ArrayList<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }
        return respond(HttpStatus.BAD_REQUEST, "Request is not valid", details);
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), status.value(), message, details));
    }
}
