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

// --- Mentor: Imports Agregados ---
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.security.JwtTokenUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.stream.Collectors;
// --- Fin Imports Agregados ---

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService; // Lo mantenemos para 'register'

    // --- Mentor: Componentes inyectados para el nuevo 'login' ---
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UsuarioRepository usuarioRepository; // Para obtener el ID

    /**
     * Mentor: MÉTODO LOGIN REFACTORIZADO
     * Autentica, obtiene roles Y permisos, y construye la respuesta completa.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        
        // 1. Autenticar (esto llama a CustomUserDetailsService)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        // 2. Establecer el contexto de seguridad
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Obtener el UserDetails (que tiene las autoridades)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 4. Generar el Token
        String token = jwtTokenUtil.generarToken(userDetails); // Asumiendo que tu util usa UserDetails

        // 5. Separar Roles de Permisos para el DTO
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
        
        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_")) // Todo lo que NO es un rol, es un permiso
                .collect(Collectors.toList());

        // 6. Obtener el ID de Usuario (la única pieza que falta)
        // (Esto requiere una consulta extra, pero es necesario para el frontend)
        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail()).get();

        // 7. Construir y devolver la respuesta completa
        AuthResponseDTO response = new AuthResponseDTO(
            token,
            usuario.getId(),
            usuario.getEmail(),
            roles,
            permisos
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     * (Este método no cambia)
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