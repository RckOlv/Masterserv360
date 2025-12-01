package com.masterserv.productos.service;

import com.masterserv.productos.dto.CambioPasswordDTO; // Nuevo DTO
import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.entity.TipoDocumento;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.mapper.ClienteMapper;
import com.masterserv.productos.repository.TipoDocumentoRepository;
import com.masterserv.productos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Importante
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    private ClienteMapper clienteMapper;
    
    // --- MENTOR: INYECCIÓN NUEVA ---
    @Autowired
    private PasswordEncoder passwordEncoder;
    // -------------------------------

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

    // --- MENTOR: MÉTODO PARA CAMBIAR PASSWORD ---
    @Transactional
    public void cambiarPassword(String email, CambioPasswordDTO dto) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Verificar que la contraseña ACTUAL sea correcta
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta.");
        }

        // 3. Encriptar y guardar la NUEVA contraseña
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }
}