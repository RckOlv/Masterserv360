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

    public AuthResponseDTO login(LoginRequestDTO request) {
        
        // 1. Autenticar (Esto verifica si el usuario y pass coinciden)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Buscar datos completos del usuario (incluyendo la bandera de cambio de pass)
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

        // --- CORRECCIÓN AQUÍ ---
        // Pasamos el 6to argumento: usuario.isDebeCambiarPassword()
        return new AuthResponseDTO(
            token, 
            usuario.getId(), 
            usuario.getEmail(), 
            roles, 
            permisos,
            usuario.isDebeCambiarPassword() // <--- ¡AQUÍ ESTABA EL FALTANTE!
        );
    }

    @Transactional
    public void register(RegisterRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El email ya está registrado.");
        }

        Rol rolPorDefecto = rolRepository.findByNombreRol("ROLE_CLIENTE") // Verifica si tu método se llama findByNombre o findByNombreRol
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
        
        // Registro normal por web no obliga cambio de pass (por defecto es false)

        usuarioRepository.save(nuevoUsuario);
    }

    // --- NUEVO MÉTODO PARA EL CAMBIO OBLIGATORIO ---
    @Transactional
    public void cambiarPasswordInicial(String email, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setDebeCambiarPassword(false); // Apagamos la alerta
        
        usuarioRepository.save(usuario);
    }
}