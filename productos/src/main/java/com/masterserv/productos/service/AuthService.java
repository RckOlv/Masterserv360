package com.masterserv.productos.service;

import com.masterserv.productos.dto.auth.AuthResponseDTO;
import com.masterserv.productos.dto.auth.LoginRequestDTO;
import com.masterserv.productos.dto.auth.RegisterRequestDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoUsuario; // Mentor: Asegúrate de tener este import
import com.masterserv.productos.repository.RolRepository;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.security.JwtTokenUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// --- Mentor: Imports Agregados ---
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
// --- Fin Imports Agregados ---

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

    /**
     * Mentor: MÉTODO LOGIN MODIFICADO
     * Autentica a un usuario y devuelve un DTO con token, roles y PERMISOS.
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        
        // 1. Autentica el usuario (Esto llama a CustomUserDetailsService)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. Obtiene los detalles del usuario (que ahora tiene roles Y permisos)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Genera el token
        String token = jwtTokenUtil.generarToken(userDetails); // Asumiendo que tu util usa UserDetails

        // 4. Separar Roles de Permisos
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toList());
        
        List<String> permisos = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_")) // Todo lo que NO es un rol
                .collect(Collectors.toList());

        // 5. Obtener el ID de Usuario (requiere una consulta)
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Error inesperado al buscar usuario post-login."));

        // 6. Retorna el DTO completo usando el nuevo constructor
        return new AuthResponseDTO(
            token,
            usuario.getId(),
            usuario.getEmail(),
            roles,
            permisos
        );
    }

    /**
     * Registra un nuevo usuario con rol CLIENTE.
     */
    @Transactional
    public void register(RegisterRequestDTO request) {
        // 1. Validar email duplicado
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El email ya está registrado.");
        }

        // 2. Buscar el rol por defecto (¡Asegúrate de que se llame ROLE_CLIENTE!)
        Rol rolPorDefecto = rolRepository.findByNombreRol("ROLE_CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error: Rol 'ROLE_CLIENTE' no encontrado."));

        // 3. Buscar el tipo de documento si se proporcionó
        TipoDocumento tipoDoc = null;
        if (request.getTipoDocumentoId() != null) {
            tipoDoc = tipoDocumentoRepository.findById(request.getTipoDocumentoId()).orElse(null);
        }

        // 4. Crear la entidad Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setDocumento(request.getDocumento());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setTipoDocumento(tipoDoc);
        nuevoUsuario.setRoles(Set.of(rolPorDefecto)); // Tu Usuario.java usa Set<Rol>
        nuevoUsuario.setEstado(EstadoUsuario.ACTIVO); // Asumo que tienes este Enum

        // 5. Guardar el usuario
        usuarioRepository.save(nuevoUsuario);
    }
}