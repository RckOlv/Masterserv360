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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

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
     * Autentica a un usuario y devuelve un token JWT.
     */
    public AuthResponseDTO login(LoginRequestDTO request) {
        // AuthenticationManager llama a nuestro CustomUserDetailsService
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Si la autenticación fue exitosa (no saltó excepción), generamos el token
        String token = jwtTokenUtil.generarToken(request.getEmail());
        
        return new AuthResponseDTO(token);
    }

    /**
     * Registra un nuevo usuario en el sistema.
     */
    @Transactional // Esta operación debe ser transaccional
    public void register(RegisterRequestDTO request) {
        
        // 1. Validar que el email no exista
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: El email ya está registrado.");
        }

        // 2. Buscar el rol por defecto ("CLIENTE" o "USUARIO")
        // IMPORTANTE: Debes tener este rol creado en tu base de datos
        Rol rolPorDefecto = rolRepository.findByNombreRol("CLIENTE")
                .orElseThrow(() -> new RuntimeException("Error: Rol 'CLIENTE' no encontrado."));

        // 3. Buscar el TipoDocumento (si se proveyó)
        TipoDocumento tipoDoc = null;
        if (request.getTipoDocumentoId() != null) {
            tipoDoc = tipoDocumentoRepository.findById(request.getTipoDocumentoId())
                    .orElse(null); // O lanzar excepción si prefieres
        }
        
        // 4. Crear la nueva entidad Usuario (Mapeo DTO -> Entidad)
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setDocumento(request.getDocumento());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setTipoDocumento(tipoDoc);
        nuevoUsuario.setRoles(Set.of(rolPorDefecto));
        nuevoUsuario.setEstado(EstadoUsuario.ACTIVO); // O PENDIENTE si tuvieras verificación

        // 5. Guardar el nuevo usuario
        usuarioRepository.save(nuevoUsuario);
    }
}