package com.masterserv.productos.controller;

import com.masterserv.productos.dto.auth.AuthResponseDTO;
import com.masterserv.productos.dto.auth.CambiarPasswordDTO; // Importar nuevo DTO
import com.masterserv.productos.dto.auth.LoginRequestDTO;
import com.masterserv.productos.dto.auth.RegisterRequestDTO;
import com.masterserv.productos.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuario registrado exitosamente"));
    }

    // --- NUEVO ENDPOINT ---
    @PostMapping("/cambiar-password-inicial")
    public ResponseEntity<?> cambiarPasswordInicial(
            @RequestBody CambiarPasswordDTO dto, 
            Principal principal) {
        
        String email = principal.getName();
        authService.cambiarPasswordInicial(email, dto.getNuevaPassword());
        
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }

    // POST /auth/forgot-password -> Body: { "email": "juan@gmail.com" }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El email es obligatorio"));
        }
        
        try {
            authService.solicitarRecuperacionPassword(email);
            return ResponseEntity.ok(Map.of("message", "Correo de recuperación enviado (si el usuario existe)."));
        } catch (RuntimeException e) {
            // Por seguridad, a veces es mejor no decir si el usuario existe o no, 
            // pero para desarrollo dejemos el mensaje de error.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    // POST /auth/reset-password -> Body: { "token": "xyz...", "password": "newPass" }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");

        if (token == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token y nueva contraseña son obligatorios"));
        }

        try {
            authService.restablecerPasswordConToken(token, password);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente. Inicia sesión."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }
}