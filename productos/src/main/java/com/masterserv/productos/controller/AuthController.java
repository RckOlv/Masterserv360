package com.masterserv.productos.controller;

import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            // ✅ Si la autenticación fue exitosa
            String token = jwtTokenUtil.generarToken(email);
            Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "token", token,
                    "usuario", usuario
            ));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "message", "Credenciales inválidas"
            ));
        }
    }

    // Opcional: para registrar usuarios (te lo dejo como referencia)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Usuario nuevoUsuario) {
        if (usuarioRepository.findByEmail(nuevoUsuario.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "El email ya está registrado"));
        }

        nuevoUsuario.setPassword(passwordEncoder.encode(nuevoUsuario.getPassword()));
        usuarioRepository.save(nuevoUsuario);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Usuario registrado exitosamente"));
    }
}
