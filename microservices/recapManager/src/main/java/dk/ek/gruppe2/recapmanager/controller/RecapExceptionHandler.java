package dk.ek.gruppe2.recapmanager.controller;

import dk.ek.gruppe2.recapmanager.exception.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AiController.class)
public class RecapExceptionHandler {
    @ExceptionHandler(StoryPathException.class)
    public ProblemDetail historyUnavailable(StoryPathException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "New journey entries could not be retrieved. The saved summary is unchanged; retry later.");
    }

    @ExceptionHandler(AiServiceException.class)
    public ProblemDetail generationFailed(AiServiceException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "The narrative could not be generated. The saved summary is unchanged.");
    }

    @ExceptionHandler({RecapConflictException.class, ObjectOptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    public ProblemDetail conflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The saved summary changed during generation. Retry using the latest saved version.");
    }
}
