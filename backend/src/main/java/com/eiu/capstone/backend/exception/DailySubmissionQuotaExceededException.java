package com.eiu.capstone.backend.exception;

/**
 * Student has reached the Vietnam-calendar daily {@code lab_submission} cap (HTTP 429).
 */
public class DailySubmissionQuotaExceededException extends RuntimeException {

    public static final String RESPONSE_HEADER = "X-Daily-Submission-Limit";
    public static final String RESPONSE_HEADER_VALUE = "1";

    public DailySubmissionQuotaExceededException(String message) {
        super(message);
    }
}
