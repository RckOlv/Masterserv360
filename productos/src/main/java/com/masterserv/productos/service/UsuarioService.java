package com.masterserv.productos.service;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO; 
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
// --- Mentor: INICIO DE IMPORTS ---
import com.masterserv.productos.entity.CuentaPuntos; // Importar CuentaPuntos
import com.masterserv.productos.repository.CuentaPuntosRepository; // Importar Repositorio
// --- Mentor: FIN DE IMPORTS ---
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.mapper.UsuarioMapper;
import com.masterserv.productos.repository.RolRepository;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.specification.UsuarioSpecification; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.Pageable; 
import org.springframework.data.jpa.domain.Specification; 
import org.springframework.security.core.context.SecurityContextHolder; // Mentor: Para el check de admin
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired
    private UsuarioMapper usuarioMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioSpecification usuarioSpecification; 

    // --- Mentor: INYECCIÓN AÑADIDA ---
    @Autowired
    private CuentaPuntosRepository cuentaPuntosRepository;
    // ---------------------------------

    // ... (filtrarUsuarios y findById quedan igual) ...

    @Transactional(readOnly = true)
    public Page<UsuarioDTO> filtrarUsuarios(UsuarioFiltroDTO filtro, Pageable pageable) {
        Specification<Usuario> spec = usuarioSpecification.getUsuariosByFilters(filtro);
        Page<Usuario> usuariosPage = usuarioRepository.findAll(spec, pageable);
        return usuariosPage.map(usuarioMapper::toUsuarioDTO); 
    }

    @Transactional(readOnly = true)
    public UsuarioDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return usuarioMapper.toUsuarioDTO(usuario);
    }


    /**
     * Crea un nuevo usuario (función de Admin).
     * MODIFICADO: Ahora también crea CuentaPuntos si el rol es CLIENTE.
     */
    @Transactional
    public UsuarioDTO crearUsuarioAdmin(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado.");
        }

        Usuario usuario = usuarioMapper.toUsuario(usuarioDTO);
        
        // 1. Hashear contraseña
        if (usuarioDTO.getPasswordHash() == null || usuarioDTO.getPasswordHash().isEmpty()) {
            throw new RuntimeException("La contraseña es obligatoria al crear un usuario.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPasswordHash()));
        
        // 2. Asignar Tipo Documento
        if (usuarioDTO.getTipoDocumentoId() != null) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(usuarioDTO.getTipoDocumentoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de Documento no encontrado."));
            usuario.setTipoDocumento(tipoDoc);
        }

        // 3. Asignar Roles
        if (usuarioDTO.getRoles() == null || usuarioDTO.getRoles().isEmpty()) {
            throw new RuntimeException("Se debe asignar al menos un rol.");
        }
        
        // --- Mentor: INICIO DE LÓGICA DE CUENTA PUNTOS ---
        boolean esCliente = false; // Flag para saber si creamos la cuenta
        
        Set<Rol> roles = new HashSet<>();
        for (RolDTO rolDto : usuarioDTO.getRoles()) {
            Rol rol = rolRepository.findById(rolDto.getId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolDto.getId()));
            roles.add(rol);
            
            if (rol.getNombreRol().equals("ROLE_CLIENTE")) {
                esCliente = true;
            }
        }
        usuario.setRoles(roles);
        
        // 4. Asignar Estado
        usuario.setEstado(usuarioDTO.getEstado() != null ? usuarioDTO.getEstado() : EstadoUsuario.ACTIVO);

        // 5. Guardar Usuario
        Usuario nuevoUsuario = usuarioRepository.save(usuario);

        // 6. Si es Cliente, crear su CuentaPuntos (LA LÓGICA QUE FALTABA)
        if (esCliente) {
            CuentaPuntos nuevaCuenta = new CuentaPuntos();
            nuevaCuenta.setCliente(nuevoUsuario);
            nuevaCuenta.setSaldoPuntos(0);
            cuentaPuntosRepository.save(nuevaCuenta);
        }
        // --- Mentor: FIN DE LÓGICA DE CUENTA PUNTOS ---

        return usuarioMapper.toUsuarioDTO(nuevoUsuario);
    }

    // ... (actualizarUsuarioAdmin y los softDelete/reactivar quedan igual) ...
    
    @Transactional
    public UsuarioDTO actualizarUsuarioAdmin(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // ... (validación de email duplicado) ...
        
        usuarioMapper.updateUsuarioFromDto(usuarioDTO, usuarioExistente);

        if (usuarioDTO.getPasswordHash() != null && !usuarioDTO.getPasswordHash().isEmpty()) {
            if (usuarioDTO.getPasswordHash().length() < 8) {
                throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres.");
            }
            usuarioExistente.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPasswordHash()));
        }

        if (usuarioDTO.getTipoDocumentoId() != null) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(usuarioDTO.getTipoDocumentoId())
                .orElseThrow(() -> new RuntimeException("Tipo de Documento no encontrado."));
            usuarioExistente.setTipoDocumento(tipoDoc);
        } else {
            usuarioExistente.setTipoDocumento(null);
        }

        if (usuarioDTO.getRoles() != null && !usuarioDTO.getRoles().isEmpty()) {
            Set<Rol> roles = usuarioDTO.getRoles().stream()
                .map(rolDto -> rolRepository.findById(rolDto.getId())
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolDto.getId())))
                .collect(Collectors.toSet());
            usuarioExistente.setRoles(roles);
        } else {
            usuarioExistente.getRoles().clear();
        }

        if(usuarioDTO.getEstado() != null) {
            usuarioExistente.setEstado(usuarioDTO.getEstado());
        }

        Usuario usuarioActualizado = usuarioRepository.save(usuarioExistente);
        return usuarioMapper.toUsuarioDTO(usuarioActualizado);
    }

    @Transactional
    public void softDelete(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // 1. Chequeo de Auto-eliminación
        String currentAuthenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (usuario.getEmail().equals(currentAuthenticatedEmail)) {
             throw new RuntimeException("No puedes desactivar tu propia cuenta mientras estás logueado.");
        }
        
        // 2. Verificar si es Admin
        boolean esAdmin = usuario.getRoles().stream()
                .anyMatch(rol -> "ROLE_ADMIN".equals(rol.getNombreRol()));
        
        // 3. Si es Admin, verificar si es el ÚLTIMO
        if (esAdmin) {
            long adminCount = usuarioRepository.countActiveAdmins(); 
            if (adminCount <= 1) { 
                throw new RuntimeException("No se puede desactivar al único Administrador activo del sistema.");
            }
        }

        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(usuario);
    }
}