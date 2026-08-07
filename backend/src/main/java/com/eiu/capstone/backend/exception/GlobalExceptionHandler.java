package com.eiu.capstone.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GoogleTokenVerificationException.class)
    public ResponseEntity<ErrorResponse> handleGoogleTokenException(GoogleTokenVerificationException exception) {
        var error = new ErrorResponse(exception.getMessage(), exception.getDetail());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request payload.");
        var error = new ErrorResponse(message, "Validation failed for request body.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException exception) {
        var error = new ErrorResponse(exception.getMessage(), "Validation failed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(SubmissionProcessingException.class)
    public ResponseEntity<ErrorResponse> handleSubmissionProcessingException(SubmissionProcessingException exception) {
        var error = new ErrorResponse(exception.getMessage(), "Submission processing failed.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        var message = exception.getReason() != null ? exception.getReason() : exception.getStatusCode().toString();
        var error = new ErrorResponse(message, "Request failed.");
        return ResponseEntity.status(exception.getStatusCode()).body(error);
    }
}
