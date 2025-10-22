package com.masterserv.productos.service;

import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Rol;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.repository.RolRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 📋 Listar todos los usuarios
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // 🧍 Registrar un cliente (rol "Cliente")
    public Usuario registrarCliente(Usuario usuario) {
        Rol rolCliente = rolRepository.findByNombre("Cliente")
                .orElseThrow(() -> new RuntimeException("Rol 'Cliente' no encontrado"));

        usuario.setRol(rolCliente);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Encripta
        usuario.setEstado(true);

        return usuarioRepository.save(usuario);
    }

    // 👨‍💼 Crear usuario con rol específico (Admin, Cliente, etc.)
    public Usuario crearUsuarioConRol(Usuario usuario) {
        if (usuario.getRol() == null) {
            throw new RuntimeException("Debe especificar un rol para el usuario");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Encripta
        usuario.setEstado(true);
        return usuarioRepository.save(usuario);
    }

    // ✏️ Editar usuario existente
    public Usuario editarUsuario(Long idUsuario, Usuario datosActualizados) {
        Usuario usuarioExistente = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioExistente.setNombre(datosActualizados.getNombre());
        usuarioExistente.setApellido(datosActualizados.getApellido());
        usuarioExistente.setDocumento(datosActualizados.getDocumento());
        usuarioExistente.setEmail(datosActualizados.getEmail());
        usuarioExistente.setEstado(datosActualizados.getEstado());

        if (datosActualizados.getPassword() != null && !datosActualizados.getPassword().isBlank()) {
            usuarioExistente.setPassword(passwordEncoder.encode(datosActualizados.getPassword()));
        }

        if (datosActualizados.getRol() != null) {
            usuarioExistente.setRol(datosActualizados.getRol());
        }

        return usuarioRepository.save(usuarioExistente);
    }

    // 🔄 Cambiar el rol de un usuario
    public Usuario cambiarRol(Long idUsuario, Long idRol) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    // 🗑️ Eliminar usuario
    public void eliminarUsuario(Long idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new RuntimeException("Usuario no encontrado");
        }
        usuarioRepository.deleteById(idUsuario);
    }
}
