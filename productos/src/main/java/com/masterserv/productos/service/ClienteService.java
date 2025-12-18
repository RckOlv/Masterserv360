package com.masterserv.productos.service;

import com.masterserv.productos.dto.CambioPasswordDTO;
import com.masterserv.productos.dto.ClienteDTO; // <--- DTO del POS
import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoUsuario;
import com.masterserv.productos.mapper.ClienteMapper;
import com.masterserv.productos.repository.RolRepository; // <--- Necesario
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class ClienteService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    private RolRepository rolRepository; // Para buscar el rol CLIENTE

    @Autowired
    private ClienteMapper clienteMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService; // Para enviar el aviso de bienvenida

    // --- MÉTODOS DE PERFIL (Existentes) ---

    @Transactional(readOnly = true)
    public ClientePerfilDTO getPerfilByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));
        
        return clienteMapper.toClientePerfilDTO(usuario);
    }

    @Transactional
    public ClientePerfilDTO updatePerfilByEmail(String email, ClientePerfilUpdateDTO updateDTO) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));

        TipoDocumento tipoDoc = tipoDocumentoRepository.findById(updateDTO.getTipoDocumentoId())
            .orElseThrow(() -> new RuntimeException("Tipo de Documento no válido: ID " + updateDTO.getTipoDocumentoId()));

        Optional<Usuario> userConMismoDoc = usuarioRepository.findByDocumento(updateDTO.getDocumento());
        if (userConMismoDoc.isPresent() && !userConMismoDoc.get().getId().equals(usuario.getId())) {
            throw new RuntimeException("El número de documento ya está registrado por otro usuario.");
        }

        clienteMapper.updateUsuarioFromDTO(updateDTO, usuario);
        usuario.setTipoDocumento(tipoDoc);
        
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        
        return clienteMapper.toClientePerfilDTO(usuarioActualizado);
    }

    // --- MÉTODO PARA CAMBIAR PASSWORD VOLUNTARIAMENTE (Desde Perfil) ---
    @Transactional
    public void cambiarPassword(String email, CambioPasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        
        // Si el usuario la cambia aquí voluntariamente, quitamos la bandera por si acaso
        usuario.setDebeCambiarPassword(false);
        
        usuarioRepository.save(usuario);
    }

    // =========================================================
    // NUEVO: REGISTRO DESDE POS (Backoffice)
    // =========================================================
    @Transactional
    public Usuario registrarClienteDesdePos(ClienteDTO dto) {
        // 1. Validaciones
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El email ya está registrado.");
        }
        if (dto.getDocumento() != null && !dto.getDocumento().isEmpty()) {
             if (usuarioRepository.findByDocumento(dto.getDocumento()).isPresent()) {
                 throw new RuntimeException("El documento ya está registrado.");
             }
        }

        // 2. Crear Entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setApellido(dto.getApellido());
        usuario.setEmail(dto.getEmail());
        usuario.setDocumento(dto.getDocumento());
        usuario.setTelefono(dto.getTelefono());
        usuario.setEstado(EstadoUsuario.ACTIVO);

        // 3. ASIGNAR TIPO DE DOCUMENTO (Lógica Mejorada)
        if (dto.getTipoDocumentoId() != null) {
            // Caso A: Viene el ID numérico
            TipoDocumento td = tipoDocumentoRepository.findById(dto.getTipoDocumentoId()).orElse(null);
            usuario.setTipoDocumento(td);
        } 
        else if (dto.getTipoDocumentoBusqueda() != null) {
            // Caso B: Viene el texto "DNI", "PAS", etc. (Lo que manda el modal)
            TipoDocumento td = tipoDocumentoRepository.findByNombreCorto(dto.getTipoDocumentoBusqueda())
                    .orElse(null); // Si no existe, queda null (no rompe)
            usuario.setTipoDocumento(td);
        }

        // 4. Asignar Rol
        Rol rolCliente = rolRepository.findByNombreRol("ROLE_CLIENTE") // Confirma si es findByNombre o findByNombreRol
                .orElseThrow(() -> new RuntimeException("Error crítico: Rol ROLE_CLIENTE no existe en BD"));
        usuario.setRoles(Set.of(rolCliente));

        // 5. Seguridad
        String passTemporal = "123456";
        usuario.setPasswordHash(passwordEncoder.encode(passTemporal));
        usuario.setDebeCambiarPassword(true); 

        // 6. Guardar
        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        // 7. Email
        try {
            enviarEmailBienvenida(usuario.getEmail(), usuario.getNombre(), passTemporal);
        } catch (Exception e) {
            System.err.println("Error enviando email: " + e.getMessage());
        }

        return usuarioGuardado;
    }

    private void enviarEmailBienvenida(String email, String nombre, String pass) {
        String asunto = "Bienvenido a Masterserv360 - Activa tu cuenta";
        String cuerpo = String.format(
            "Hola %s,\n\n" +
            "Te hemos registrado en nuestro sistema.\n" +
            "Tus credenciales temporales son:\n\n" +
            "Usuario: %s\n" +
            "Contraseña: %s\n\n" +
            "⚠️ IMPORTANTE: Por seguridad, deberás cambiar esta contraseña al ingresar por primera vez.\n" +
            "Ingresa aquí: http://localhost:4200/login\n\n" +
            "Saludos,\nEl equipo de Masterserv360",
            nombre, email, pass
        );
        
        emailService.enviarEmail(email, asunto, cuerpo);
    }
}