package com.masterserv.productos.service;

import com.masterserv.productos.dto.auth.AuthResponseDTO;
import com.masterserv.productos.dto.auth.LoginRequestDTO;
import com.masterserv.productos.dto.auth.RegisterRequestDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.repository.RolRepository;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.masterserv.productos.entity.PasswordResetToken;
import com.masterserv.productos.repository.PasswordResetTokenRepository;
import java.util.UUID;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    // --- NUEVO: Inyectar EmailService ---
    @Autowired
    private EmailService emailService; 
    // ------------------------------------

    public AuthResponseDTO login(LoginRequestDTO request) {
        // ... (Tu código de login sigue igual) ...
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String token = jwtTokenUtil.generateToken(usuario);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
        
        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toList());

        return new AuthResponseDTO(
            token, 
            usuario.getId(), 
            usuario.getEmail(), 
            roles, 
            permisos,
            usuario.isDebeCambiarPassword() 
        );
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El email ya está registrado.");
        }

        Rol rolPorDefecto = rolRepository.findByNombreRol("ROLE_CLIENTE") 
                .orElseThrow(() -> new RuntimeException("Error: Rol 'ROLE_CLIENTE' no encontrado."));

        TipoDocumento tipoDoc = null;
        if (request.getTipoDocumentoId() != null) {
            tipoDoc = tipoDocumentoRepository.findById(request.getTipoDocumentoId()).orElse(null);
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setDocumento(request.getDocumento());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setTipoDocumento(tipoDoc);
        nuevoUsuario.setRoles(Set.of(rolPorDefecto));
        nuevoUsuario.setEstado(EstadoUsuario.ACTIVO);
        
        usuarioRepository.save(nuevoUsuario);

        // --- NUEVO: Enviar Correo de Bienvenida ---
        try {
            emailService.sendWelcomeEmail(nuevoUsuario.getEmail(), nuevoUsuario.getNombre());
            System.out.println("📧 Correo de bienvenida enviado a: " + nuevoUsuario.getEmail());
        } catch (Exception e) {
            // Importante: No queremos que falle el registro si falla el correo.
            // Solo lo logueamos como error.
            System.err.println("❌ Error enviando correo de bienvenida: " + e.getMessage());
        }
        // ------------------------------------------
    }

    @Transactional
    public void cambiarPasswordInicial(String email, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setDebeCambiarPassword(false); 
        
        usuarioRepository.save(usuario);
    }

    // 1. SOLICITAR TOKEN (Paso 1)
    @Transactional
    public void solicitarRecuperacionPassword(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe un usuario con ese email."));

        // Borrar tokens viejos si el usuario ya había pedido uno antes
        tokenRepository.deleteByUsuario(usuario);

        // Crear token nuevo
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, usuario);
        tokenRepository.save(resetToken);

        // Enviar email
        try {
            emailService.sendPasswordResetEmail(usuario.getEmail(), usuario.getNombre(), token);
        } catch (Exception e) {
            throw new RuntimeException("Error enviando el correo. Intenta más tarde.");
        }
    }

    // 2. CAMBIAR PASSWORD CON TOKEN (Paso 2)
    @Transactional
    public void restablecerPasswordConToken(String token, String nuevaPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("El enlace es inválido o ha expirado."));

        if (resetToken.estaExpirado()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("El enlace ha expirado. Solicita uno nuevo.");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        // Consumir el token (borrarlo para que no se use dos veces)
        tokenRepository.delete(resetToken);
    }
}