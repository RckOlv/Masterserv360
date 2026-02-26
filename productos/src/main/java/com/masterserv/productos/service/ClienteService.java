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
        String asunto = "Tus accesos a Masterserv360 🔐";
        String urlFrontend = "https://masterserv360.vercel.app/login";

        String cuerpoHtml = """
            <div style="font-family: Arial, sans-serif; color: #fff; background-color: #121212; max-width: 600px; margin: auto; border: 1px solid #333; border-radius: 8px; overflow: hidden;">
                <div style="background-color: #dc3545; padding: 20px; text-align: center;">
                    <h2 style="color: white; margin: 0;">Masterserv360</h2>
                </div>
                <div style="padding: 20px; background-color: #1a1a1a;">
                    <h3 style="color: #fff; margin-top: 0;">¡Hola %s! 👋</h3>
                    <p style="color: #ccc; font-size: 15px;">Te hemos registrado exitosamente en nuestro sistema. Aquí tienes tus credenciales de acceso temporal:</p>
                    
                    <div style="background-color: #2a2a2a; padding: 15px; border-left: 4px solid #0dcaf0; margin: 20px 0; border-radius: 4px;">
                        <p style="margin: 5px 0; color: #fff; font-size: 16px;"><strong>Usuario:</strong> %s</p>
                        <p style="margin: 5px 0; color: #fff; font-size: 16px;"><strong>Contraseña:</strong> %s</p>
                    </div>
                    
                    <div style="background-color: #ffc107; color: #000; padding: 12px; border-radius: 4px; font-size: 13px; font-weight: bold; margin-bottom: 25px; text-align: center;">
                        ⚠️ IMPORTANTE: Por seguridad, el sistema te pedirá cambiar esta contraseña al ingresar por primera vez.
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background-color: #dc3545; color: white; padding: 14px 28px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block; letter-spacing: 1px;">Ingresar al Sistema</a>
                    </div>
                    
                    <p style="font-size: 12px; color: #777; border-top: 1px solid #333; padding-top: 15px; text-align: center;">
                        Saludos,<br>El equipo de Masterserv360
                    </p>
                </div>
            </div>
            """.formatted(nombre, email, pass, urlFrontend);
        emailService.enviarEmailHtml(email, asunto, cuerpoHtml);
    }
}
