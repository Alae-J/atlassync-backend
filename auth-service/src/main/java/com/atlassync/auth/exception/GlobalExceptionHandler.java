package com.atlassync.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/duplicate-resource"));
        problem.setTitle("Duplicate Resource");
        return problem;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/bad-credentials"));
        problem.setTitle("Authentication Failed");
        return problem;
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/invalid-token"));
        problem.setTitle("Invalid Token");
        return problem;
    }

    @ExceptionHandler(TokenReuseException.class)
    public ProblemDetail handleTokenReuse(TokenReuseException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/token-reuse"));
        problem.setTitle("Token Reuse Detected");
        return problem;
    }

    @ExceptionHandler(OtpInvalidCodeException.class)
    public ProblemDetail handleOtpInvalid(OtpInvalidCodeException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/otp-invalid-code"));
        problem.setTitle("Invalid OTP Code");
        return problem;
    }

    @ExceptionHandler(OtpChallengeExpiredException.class)
    public ProblemDetail handleOtpExpired(OtpChallengeExpiredException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/otp-expired"));
        problem.setTitle("OTP Challenge Expired");
        return problem;
    }

    @ExceptionHandler(OtpRateLimitedException.class)
    public ResponseEntity<ProblemDetail> handleOtpRateLimit(OtpRateLimitedException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://atlassync.com/errors/otp-rate-limited"));
        problem.setTitle("Too Many OTP Requests");
        long retryAfterSeconds = ex.getRetryAfter().toSeconds();
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid",
                        (first, second) -> first
                ));

        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(URI.create("https://atlassync.com/errors/validation"));
        problem.setTitle("Validation Error");
        problem.setProperty("errors", fieldErrors);
        return problem;
    }
}
