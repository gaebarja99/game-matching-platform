package com.gamematcher.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global API exception handling.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "입력값 검증 실패",
                        "message", message,
                        "details", message,
                        "status", 400
                ));
    }

    @ExceptionHandler(GameApiException.class)
    public ResponseEntity<Map<String, Object>> handleGameApiException(GameApiException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage(),
                        "status", e.getStatus().value()
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", e.getMessage(),
                        "message", e.getMessage(),
                        "status", 500
                ));
    }
}
