package org.example.delivery.exceptions.handler;

import lombok.extern.slf4j.Slf4j;
import org.example.delivery.exceptions.response.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import org.example.delivery.exceptions.WeatherDataFetchException;

@ControllerAdvice
@Slf4j
public class ErrorHandler {

    /**
     * Handles uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponse> handleException(Exception ex) {
        log.error("Internal server error", ex);
        return new ResponseEntity<>(new ExceptionResponse("Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalArgumentException(Exception ex) {
        return new ResponseEntity<>(new ExceptionResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ExceptionResponse> handleIllegalStateException(Exception ex) {
        return new ResponseEntity<>(new ExceptionResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ExceptionResponse> handleBadRequest(Exception ex) {
        return new ResponseEntity<>(new ExceptionResponse(messageOrDefault(ex, "Invalid request")),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return new ResponseEntity<>(new ExceptionResponse("Request could not be saved"),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(WeatherDataFetchException.class)
    public ResponseEntity<ExceptionResponse> handleWeatherDataFetch(WeatherDataFetchException ex) {
        log.error("Weather data import failed", ex);
        return new ResponseEntity<>(new ExceptionResponse(ex.getMessage()), HttpStatus.BAD_GATEWAY);
    }

    private String messageOrDefault(Exception ex, String fallback) {
        return ex.getMessage() == null || ex.getMessage().isBlank() ? fallback : ex.getMessage();
    }

}
