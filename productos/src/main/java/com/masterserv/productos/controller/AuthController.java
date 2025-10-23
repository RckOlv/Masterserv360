package com.masterserv.productos.controller;

import com.masterserv.productos.dto.auth.AuthResponseDTO;
import com.masterserv.productos.dto.auth.LoginRequestDTO;
import com.masterserv.productos.dto.auth.RegisterRequestDTO;
import com.masterserv.productos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200") // Mantenemos tu CORS
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Endpoint para autenticar un usuario y obtener un token JWT.
     * Usa DTOs para request y response, y @Valid para validación.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // Delegamos TODA la lógica al servicio
        AuthResponseDTO response = authService.login(loginRequest);
        
        // NO devolvemos la entidad Usuario. Solo el token.
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     * Usa un DTO para el request y @Valid para validación.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        
        // Delegamos TODA la lógica al servicio
        authService.register(registerRequest);
        
        return ResponseEntity
                .status(HttpStatus.CREATED) // 201 Created es la respuesta correcta
                .body(Map.of("message", "Usuario registrado exitosamente"));
    }
}