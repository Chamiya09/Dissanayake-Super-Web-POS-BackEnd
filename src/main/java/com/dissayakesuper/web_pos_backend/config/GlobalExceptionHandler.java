package com.dissayakesuper.web_pos_backend.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Centralised error handler — guarantees every API error response contains
 * a human-readable {@code message} field, regardless of Spring Boot's
 * {@code server.error.include-message} setting.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /* ── Bean-validation failures (400) ─────────────────────────────────────── */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body(400, message));
    }

    /* ── ResponseStatusException (any status code) ──────────────────────────── */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(status).body(body(status, message));
    }

    /* ── Missing endpoint/static resource (404) ───────────────────────────── */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(404, "Endpoint not found: " + ex.getResourcePath()));
    }

    /* ── Catch-all (500) ────────────────────────────────────────────────────── */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        ex.printStackTrace();                       // log full trace for debugging
        String message = ex.getMessage() != null ? ex.getMessage() : "Unexpected server error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(500, message));
    }

    private static Map<String, Object> body(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("message", message);
        return body;
    }
}
