package com.atlassync.session.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ProblemDetail handleIllegalStateTransition(IllegalStateTransitionException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Illegal State Transition");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail handleSessionNotFound(SessionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Session Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidQrTokenException.class)
    public ProblemDetail handleInvalidQrToken(InvalidQrTokenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid QR Token");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(QrTokenExpiredException.class)
    public ProblemDetail handleQrTokenExpired(QrTokenExpiredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setTitle("QR Token Expired");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
