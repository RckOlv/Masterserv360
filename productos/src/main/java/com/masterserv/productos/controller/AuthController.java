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
}