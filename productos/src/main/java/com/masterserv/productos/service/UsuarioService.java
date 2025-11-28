package com.masterserv.productos.service;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO; 
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.CuentaPuntos; 
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.mapper.UsuarioMapper;
import com.masterserv.productos.repository.RolRepository;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.CuentaPuntosRepository; 
import com.masterserv.productos.specification.UsuarioSpecification; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.Pageable; 
import org.springframework.data.jpa.domain.Specification; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional; // Importante
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private UsuarioMapper usuarioMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioSpecification usuarioSpecification; 
    @Autowired private CuentaPuntosRepository cuentaPuntosRepository;

    // ... (tus métodos filtrarUsuarios, findById, crearUsuarioAdmin, actualizar, etc. déjalos igual) ...
    
    // TE DEJO AQUÍ LOS MÉTODOS DE LECTURA Y ADMIN PARA QUE NO SE PIERDAN,
    // PERO LA CORRECCIÓN IMPORTANTE ESTÁ AL FINAL EN 'crearClienteRapido'

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

    @Transactional
    public UsuarioDTO crearUsuarioAdmin(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado.");
        }

        Usuario usuario = usuarioMapper.toUsuario(usuarioDTO);
        
        if (usuarioDTO.getPasswordHash() == null || usuarioDTO.getPasswordHash().isEmpty()) {
            throw new RuntimeException("La contraseña es obligatoria al crear un usuario.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPasswordHash()));
        
        if (usuarioDTO.getTipoDocumentoId() != null) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(usuarioDTO.getTipoDocumentoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de Documento no encontrado."));
            usuario.setTipoDocumento(tipoDoc);
        }

        if (usuarioDTO.getRoles() == null || usuarioDTO.getRoles().isEmpty()) {
            throw new RuntimeException("Se debe asignar al menos un rol.");
        }
        
        boolean esCliente = false; 
        Set<Rol> roles = new HashSet<>();
        
        for (RolDTO rolDto : usuarioDTO.getRoles()) {
            Rol rol = rolRepository.findById(rolDto.getId())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolDto.getId()));
            roles.add(rol);
            
            // Verificamos si es cliente para crear cuenta puntos
            String nombreRol = rol.getNombreRol().toUpperCase();
            if (nombreRol.equals("ROLE_CLIENTE") || nombreRol.equals("CLIENTE")) {
                esCliente = true;
            }
        }
        usuario.setRoles(roles);
        
        usuario.setEstado(usuarioDTO.getEstado() != null ? usuarioDTO.getEstado() : EstadoUsuario.ACTIVO);
        usuario.setFechaCreacion(LocalDateTime.now());

        Usuario nuevoUsuario = usuarioRepository.save(usuario);

        if (esCliente) {
             if (!cuentaPuntosRepository.existsByClienteId(nuevoUsuario.getId())) {
                 CuentaPuntos nuevaCuenta = new CuentaPuntos();
                 nuevaCuenta.setCliente(nuevoUsuario);
                 nuevaCuenta.setSaldoPuntos(0);
                 cuentaPuntosRepository.save(nuevaCuenta);
             }
        }

        return usuarioMapper.toUsuarioDTO(nuevoUsuario);
    }

    @Transactional
    public UsuarioDTO actualizarUsuarioAdmin(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

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

        String currentAuthenticatedEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        if (usuario.getEmail().equals(currentAuthenticatedEmail)) {
             throw new RuntimeException("No puedes desactivar tu propia cuenta mientras estás logueado.");
        }
        
        boolean esAdmin = usuario.getRoles().stream()
                .anyMatch(rol -> "ROLE_ADMIN".equals(rol.getNombreRol()));
        
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

    /**
     * ALTA RÁPIDA (POS) - CORREGIDO
     */
    @Transactional
    public UsuarioDTO crearClienteRapido(UsuarioDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya está registrado.");
        }
        if (usuarioRepository.existsByDocumento(dto.getDocumento())) {
            throw new RuntimeException("El documento ya está registrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setDocumento(dto.getDocumento());
        usuario.setEmail(dto.getEmail());
        usuario.setTelefono(dto.getTelefono());
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setFechaCreacion(LocalDateTime.now());

        // Buscar por nombre (DNI, CUIT, etc.)
        if (dto.getTipoDocumentoBusqueda() != null) {
             TipoDocumento tipoDoc = tipoDocumentoRepository.findByNombreCorto(dto.getTipoDocumentoBusqueda())
                    .orElseThrow(() -> new RuntimeException("Tipo de documento '" + dto.getTipoDocumentoBusqueda() + "' no encontrado."));
            usuario.setTipoDocumento(tipoDoc);
        } 
        // Fallback: si manda ID (como antes)
        else if (dto.getTipoDocumentoId() != null) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(dto.getTipoDocumentoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de documento inválido (ID no encontrado)"));
            usuario.setTipoDocumento(tipoDoc);
        }

        usuario.setPasswordHash(passwordEncoder.encode("123456")); 
        
        // --- CORRECCIÓN: Búsqueda Robusta de Rol ---
        // 1. Intentamos buscar "ROLE_CLIENTE" (nombre estándar)
        Optional<Rol> rolOpt = rolRepository.findByNombreRol("ROLE_CLIENTE");
        
        // 2. Si no existe, intentamos buscar "CLIENTE"
        if (rolOpt.isEmpty()) {
            rolOpt = rolRepository.findByNombreRol("CLIENTE");
        }
        
        // 3. Si aún así no existe, lanzamos un error claro
        Rol rolCliente = rolOpt.orElseThrow(() -> new RuntimeException("Error crítico de configuración: No existe el rol 'ROLE_CLIENTE' ni 'CLIENTE' en la base de datos."));
        // --------------------------------------------
        
        Set<Rol> roles = new HashSet<>();
        roles.add(rolCliente);
        usuario.setRoles(roles);

        Usuario guardado = usuarioRepository.save(usuario);
        
        // Crear cuenta de puntos
        if (!cuentaPuntosRepository.existsByClienteId(guardado.getId())) {
             CuentaPuntos nuevaCuenta = new CuentaPuntos();
             nuevaCuenta.setCliente(guardado);
             nuevaCuenta.setSaldoPuntos(0);
             cuentaPuntosRepository.save(nuevaCuenta);
        }

        return usuarioMapper.toUsuarioDTO(guardado);
    }
}