package com.eiu.capstone.backend.exception;

public class SubmissionProcessingException extends RuntimeException {

    public SubmissionProcessingException(String message) {
        super(message);
    }

    public SubmissionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}