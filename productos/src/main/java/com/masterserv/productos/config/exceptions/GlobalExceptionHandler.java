package com.masterserv.productos.config.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja las credenciales inválidas (Login).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(Map.of("status", "error", "message", "Credenciales inválidas"));
    }

    /**
     * Maneja los errores de validación de DTOs (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = Map.of(
            "status", "error",
            "message", "Error de validación",
            "errors", ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> Map.of("field", err.getField(), "defaultMessage", err.getDefaultMessage()))
                        .collect(Collectors.toList())
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST); // 400
    }

    /**
     * Maneja los errores de negocio (ej. "Email ya existe").
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleBusinessException(RuntimeException e) {
        // En un proyecto más grande, crearíamos excepciones custom (ej. EmailExistsException)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("status", "error", "message", e.getMessage()));
    }
}