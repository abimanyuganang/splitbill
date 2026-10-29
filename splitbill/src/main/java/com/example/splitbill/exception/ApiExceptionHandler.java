
package com.example.splitbill.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
        IllegalArgumentException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 400,
            "error", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                error.getField() + ": " +
                error.getDefaultMessage()
            )
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 400,
                "error", message
            ));
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, Object>> handleAmountError(
        ArithmeticException ex
    ) {
        return ResponseEntity.badRequest().body(Map.of(
            "timestamp", Instant.now().toString(),
            "status", 400,
            "error", "Amount must have at most 2 decimal places"
        ));
    }
}
