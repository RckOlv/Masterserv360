package com.masterserv.productos.exceptions;

// Añadimos el Logger para los errores de Nivel 3
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest; // Necesario para loguear

// --- Mentor: ¡IMPORTE AGREGADO! ---
// Necesitamos importar la excepción que queremos atrapar.
import jakarta.persistence.EntityNotFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Nivel 3: Creamos un logger para los errores internos
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Nivel 1: Maneja las credenciales inválidas (Login).
     * Devuelve 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(Map.of("status", "error", "message", "Credenciales inválidas"));
    }

    /**
     * Nivel 1: Maneja los errores de validación de DTOs (@Valid).
     * Devuelve 400 Bad Request.
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

    // --- Mentor: ¡NUEVO MANEJADOR DE ERROR! ---
    /**
     * Nivel 2: Maneja errores de "recurso no encontrado" (ej. Categoría 9999).
     * Lanzado por .orElseThrow() en los servicios.
     * Devuelve 404 Not Found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFoundException(EntityNotFoundException e) {
        // 404 Not Found es el código correcto para "el recurso que pediste no existe"
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(Map.of("status", "error", "message", e.getMessage()));
    }
    // --- FIN DEL NUEVO MANEJADOR ---


    /**
     * Nivel 2: Maneja errores de negocio CONFLICTIVOS (ej. "Email ya existe", "Código duplicado").
     * Usamos IllegalArgumentException porque es lo que lanzamos en ProductoService.
     * Devuelve 409 Conflict.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleConflictException(IllegalArgumentException e) {
        // 409 Conflict es semánticamente mejor para "recurso duplicado"
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(Map.of("status", "error", "message", e.getMessage()));
    }

    /**
     * Nivel 2: Maneja reglas de negocio específicas (Stock, Cupones).
     * Estos son errores "esperados" que el usuario puede corregir.
     * Devuelve 400 Bad Request.
     */
    @ExceptionHandler({StockInsuficienteException.class, CuponNoValidoException.class})
    public ResponseEntity<Map<String, String>> handleBadRequestBusinessExceptions(RuntimeException e) {
        // 400 Bad Request es correcto, el cliente pidió algo imposible (ej. más stock del disponible)
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("status", "error", "message", e.getMessage()));
    }

    /**
     * Nivel 3: ¡El "Catch-All" de seguridad!
     * Maneja CUALQUIER OTRA excepción no esperada (NPE, DB errors, etc.).
     * Devuelve 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class) // Capturamos Exception.class (el más genérico)
    public ResponseEntity<Map<String, String>> handleAllUncaughtException(Exception e, WebRequest request) {
        
        // 1. ¡LOGUEAR EL ERROR! (Vital para debugging)
        // Esto se imprime en la consola del servidor, no se envía al cliente.
        logger.error("Error no controlado en la solicitud: {}", request.getDescription(false), e);
        // El 'e' al final imprime el StackTrace completo en tus logs.

        // 2. Devolvemos un 500 genérico al cliente.
        // ¡NUNCA enviar e.getMessage() de un error 500!
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR) // 500
                .body(Map.of("status", "error", "message", "Ocurrió un error interno en el servidor."));
    }
}