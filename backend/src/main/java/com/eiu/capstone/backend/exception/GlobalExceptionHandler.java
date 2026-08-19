package com.eiu.capstone.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.eiu.capstone.backend.model.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        var message = extractConstraintMessage(exception);
        var error = new ErrorResponse(message, "Database constraint violated.");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    private static String extractConstraintMessage(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String detail = cause != null ? cause.getMessage() : exception.getMessage();
        if (detail == null) {
            return "Could not save testcase data. Check assertion kinds, field references, and JSON values.";
        }
        if (detail.contains("testcase_assertion_field_check")) {
            return "FIELD_STATE assertions require a field; other assertion kinds must not reference a field.";
        }
        if (detail.contains("testcase_invocation_kind_check")) {
            return "Invalid invocation configuration for the selected constructor or method.";
        }
        if (detail.contains("receiver_params") || detail.contains("receiver_constructor_id")) {
            return "Database schema is missing testcase invocation receiver columns. Run docs/sql/2026-08-11-testcase-invocation-receiver.sql.";
        }
        if (detail.contains("violates foreign key constraint")) {
            return "A referenced constructor, method, or field does not exist. Save lab structure first, then retry.";
        }
        return "Could not save testcase data. Check assertion kinds, field references, and JSON values.";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        var error = new ErrorResponse("Not found", "No matching API route or resource.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandledException(Exception exception) {
        log.error("Unhandled request failure", exception);
        var error = new ErrorResponse("Internal server error", "Internal server error.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
