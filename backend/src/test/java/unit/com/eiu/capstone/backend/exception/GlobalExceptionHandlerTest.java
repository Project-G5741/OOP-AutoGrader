package unit.com.eiu.capstone.backend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.eiu.capstone.backend.exception.GlobalExceptionHandler;
import com.eiu.capstone.backend.model.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maxUploadSize_returns413WithClearMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(50L * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals(
                "This file is too large to upload (limit 50MB). Use a smaller practice pack or project folder.",
                response.getBody().message());
    }
}
