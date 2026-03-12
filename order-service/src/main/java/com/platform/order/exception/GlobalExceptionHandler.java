package com.platform.order.exception;

import com.platform.shared.dto.ErrorResponse;
import com.platform.shared.exception.IdempotencyConflictException;
import com.platform.shared.exception.OrderNotFoundException;
import com.platform.shared.exception.PartnerRevokedException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PartnerRevokedException.class)
    public ResponseEntity<ErrorResponse> handlePartnerRevoked(
            PartnerRevokedException ex) {
        log.warn("Partner revoked access attempt: {}", ex.getPartnerId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ErrorResponse.builder()
                        .errorCode("PARTNER_REVOKED")
                        .message("Partner access has been revoked")
                        .correlationId(MDC.get("correlationId"))
                        .build()
        );
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
            IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .errorCode("IDEMPOTENCY_CONFLICT")
                        .message("Idempotency key reused with different payload")
                        .correlationId(MDC.get("correlationId"))
                        .build()
        );
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex) {

        log.warn("Order not found: {} correlationId={}",
                ex.getMessage(), MDC.get("correlationId"));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .errorCode("ORDER_NOT_FOUND")
                        .message(ex.getMessage())
                        .correlationId(MDC.get("correlationId"))
                        .build()
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("MISSING_HEADER")
                        .message("Required header missing: " + ex.getHeaderName())
                        .correlationId(MDC.get("correlationId"))
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            details.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("VALIDATION_ERROR")
                        .message("Request validation failed")
                        .correlationId(MDC.get("correlationId"))
                        .details(details)
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .errorCode("INTERNAL_ERROR")
                        .message("An unexpected error occurred")
                        .correlationId(MDC.get("correlationId"))
                        .build()
        );
    }
}