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
// @CrossOrigin(origins = "http://localhost:4200") // Ya configurado globalmente en SecurityConfig
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Inicia sesión y devuelve el token JWT con los datos del usuario.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        // Delegamos toda la lógica al servicio (Clean Architecture)
        AuthResponseDTO response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Registra un nuevo cliente.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        authService.register(registerRequest);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Usuario registrado exitosamente"));
    }
}