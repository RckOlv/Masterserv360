package com.masterserv.productos.service;

import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.mapper.ClienteMapper;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository; // Para validar el ID del documento

    @Autowired
    private ClienteMapper clienteMapper;

    /**
     * Obtiene la información del perfil de un cliente por su email.
     *
     * @param email Email del cliente (del Principal).
     * @return El DTO del perfil.
     */
    @Transactional(readOnly = true)
    public ClientePerfilDTO getPerfilByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));
        
        return clienteMapper.toClientePerfilDTO(usuario);
    }

    /**
     * Actualiza el perfil de un cliente.
     *
     * @param email Email del cliente (del Principal).
     * @param updateDTO DTO con los datos a actualizar.
     * @return El DTO del perfil ya actualizado.
     */
    @Transactional
    public ClientePerfilDTO updatePerfilByEmail(String email, ClientePerfilUpdateDTO updateDTO) {
        
        // 1. Buscar al usuario a actualizar
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado: " + email));

        // 2. Validar que el TipoDocumento exista
        TipoDocumento tipoDoc = tipoDocumentoRepository.findById(updateDTO.getTipoDocumentoId())
            .orElseThrow(() -> new RuntimeException("Tipo de Documento no válido: ID " + updateDTO.getTipoDocumentoId()));

        // 3. (Lógica de Negocio Opcional) Validar si el DNI ya está en uso por OTRO usuario
        Optional<Usuario> userConMismoDoc = usuarioRepository.findByDocumento(updateDTO.getDocumento());
        if (userConMismoDoc.isPresent() && !userConMismoDoc.get().getId().equals(usuario.getId())) {
            throw new RuntimeException("El número de documento ya está registrado por otro usuario.");
        }

        // 4. Usar el mapper para actualizar los campos simples (nombre, apellido, etc.)
        clienteMapper.updateUsuarioFromDTO(updateDTO, usuario);
        
        // 5. Asignar la entidad completa (ya validada)
        usuario.setTipoDocumento(tipoDoc);
        
        // 6. Guardar y devolver
        Usuario usuarioActualizado = usuarioRepository.save(usuario);
        
        return clienteMapper.toClientePerfilDTO(usuarioActualizado);
    }
}