package com.masterserv.productos.service;

import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO; // Importado
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.mapper.UsuarioMapper;
import com.masterserv.productos.repository.RolRepository;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.specification.UsuarioSpecification; // Importado
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importado
import org.springframework.data.domain.Pageable; // Importado
import org.springframework.data.jpa.domain.Specification; // Importado
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
    private UsuarioSpecification usuarioSpecification; // <-- AÑADIDO

    /**
     * MODIFICADO: Ahora filtra, pagina y soluciona N+1.
     */
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> filtrarUsuarios(UsuarioFiltroDTO filtro, Pageable pageable) {
        Specification<Usuario> spec = usuarioSpecification.getUsuariosByFilters(filtro);
        Page<Usuario> usuariosPage = usuarioRepository.findAll(spec, pageable);
        return usuariosPage.map(usuarioMapper::toUsuarioDTO); 
    }

    /**
     * Busca un usuario por ID (para editar).
     */
    @Transactional(readOnly = true)
    public UsuarioDTO findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
        return usuarioMapper.toUsuarioDTO(usuario);
    }

    /**
     * Crea un nuevo usuario (función de Admin).
     */
    @Transactional
    public UsuarioDTO crearUsuarioAdmin(UsuarioDTO usuarioDTO) {
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            throw new RuntimeException("El email ya está registrado.");
        }

        Usuario usuario = usuarioMapper.toUsuario(usuarioDTO);
        
        // 1. Hashear contraseña (obligatoria al crear)
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

        // 3. Asignar Roles (el DTO debe traer los IDs en el Set<RolDTO>)
        if (usuarioDTO.getRoles() == null || usuarioDTO.getRoles().isEmpty()) {
            throw new RuntimeException("Se debe asignar al menos un rol.");
        }
        Set<Rol> roles = usuarioDTO.getRoles().stream()
                .map(rolDto -> rolRepository.findById(rolDto.getId())
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolDto.getId())))
                .collect(Collectors.toSet());
        usuario.setRoles(roles);
        
        // 4. Asignar Estado
        usuario.setEstado(usuarioDTO.getEstado() != null ? usuarioDTO.getEstado() : EstadoUsuario.ACTIVO);

        Usuario nuevoUsuario = usuarioRepository.save(usuario);
        return usuarioMapper.toUsuarioDTO(nuevoUsuario);
    }

    /**
     * Actualiza un usuario existente (función de Admin).
     */
   @Transactional
    public UsuarioDTO actualizarUsuarioAdmin(Long id, UsuarioDTO usuarioDTO) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // ... (validación de email duplicado) ...
        
        // Mapea los campos básicos (Nombre, Apellido, Email, Doc, Tel)
        // (Asegúrate de que tu UsuarioMapper.java tenga "updateUsuarioFromDto")
        usuarioMapper.updateUsuarioFromDto(usuarioDTO, usuarioExistente);

        // --- ¡AQUÍ ESTÁ LA LÓGICA DE CONTRASEÑA! ---
        // Actualiza contraseña SOLO SI se envió una nueva
        if (usuarioDTO.getPasswordHash() != null && !usuarioDTO.getPasswordHash().isEmpty()) {
            // Validamos el tamaño aquí, no en el DTO
            if (usuarioDTO.getPasswordHash().length() < 8) { // O el min que definiste en el form
                throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres.");
            }
            usuarioExistente.setPasswordHash(passwordEncoder.encode(usuarioDTO.getPasswordHash()));
        }
        // Si es null o vacía, NO HACE NADA (conserva la contraseña antigua)
        // ------------------------------------------

        // Actualiza Tipo Documento
        if (usuarioDTO.getTipoDocumentoId() != null) {
            TipoDocumento tipoDoc = tipoDocumentoRepository.findById(usuarioDTO.getTipoDocumentoId())
                .orElseThrow(() -> new RuntimeException("Tipo de Documento no encontrado."));
            usuarioExistente.setTipoDocumento(tipoDoc);
        } else {
            usuarioExistente.setTipoDocumento(null); // Permite quitarlo
        }

        // Actualiza Roles
        if (usuarioDTO.getRoles() != null && !usuarioDTO.getRoles().isEmpty()) {
            Set<Rol> roles = usuarioDTO.getRoles().stream()
                .map(rolDto -> rolRepository.findById(rolDto.getId())
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + rolDto.getId())))
                .collect(Collectors.toSet());
            usuarioExistente.setRoles(roles);
        } else {
            // Opcional: ¿Permitir quitar todos los roles?
            usuarioExistente.getRoles().clear();
        }

        // Actualiza Estado
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