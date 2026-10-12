package com.maz.my_hotel.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        ErrorResponse body = null;
        if (isDevOrStaging) {
            body = ErrorResponse.of(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    "ResourceNotFoundException | " + ex.getMessage());
        } else {
            body = ErrorResponse.of(
                    HttpStatus.NOT_FOUND.value(),
                    HttpStatus.NOT_FOUND.getReasonPhrase(),
                    ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach((fieldError) ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        log.warn("Validation failed: {}", fieldErrors);


        ErrorResponse body = null;
        if (isDevOrStaging) {
            body = ErrorResponse.ofValidation(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "MethodArgumentNotValidException | Validation failed",
                    fieldErrors);
        } else {
            body = ErrorResponse.ofValidation(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Validation failed",
                    fieldErrors);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        ErrorResponse body = null;
        if (isDevOrStaging) {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "DataIntegrityViolationException | Data integrity violation: " + ex.getMostSpecificCause().getMessage());
        } else {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "Data integrity violation: " + ex.getMostSpecificCause().getMessage());
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        log.warn("Business rule violated: {}", ex.getMessage());

        ErrorResponse body = null;
        if (isDevOrStaging) {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "BusinessRuleException | " + ex.getMessage());
        } else {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        log.warn("Method argument type mismatch: {}", ex.getMessage());

        ErrorResponse body = null;
        if (isDevOrStaging) {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    "MethodArgumentTypeMismatchException | " + ex.getMessage());
        } else {
            body = ErrorResponse.of(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.getReasonPhrase(),
                    ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Http code 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        // Log an unexpected application failure.
        //
        // Logback automatically adds requestId and correlationId
        // from MDC when those values are available.
        log.error("Unexpected application failure: {}", ex.getMessage(), ex);

        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrStaging = false;
        for (String profile : activeProfiles) {
            if("dev".equals(profile) || "staging".equals(profile)) {
                isDevOrStaging = true;
                break;
            }
        }

        ErrorResponse body = null;
        if (isDevOrStaging) {
            String message = "An unexpected error occurred";
            if (!ex.getClass().getName().isBlank() || !ex.getMessage().isBlank()) {
                message = "Exception: " + ex.getClass().getName() + " | Message: " + ex.getMessage();
            }
            body = ErrorResponse.of(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    message);
        } else {
            body = ErrorResponse.of(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    "An unexpected error occurred. Please try again later.");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
