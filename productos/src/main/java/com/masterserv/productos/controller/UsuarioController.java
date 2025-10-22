package com.masterserv.productos.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public List<Usuario> listar() {
        return usuarioService.listarUsuarios();
    }

    // Registro de cliente (rol fijo CLIENTE)
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrarCliente(@RequestBody Usuario usuario) {
        Usuario nuevo = usuarioService.registrarCliente(usuario);
        return ResponseEntity.ok(nuevo);
    }

    // Crear usuario con rol elegido (usado por admin o registro con rol)
    @PostMapping("/crear")
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario usuario) {
        Usuario nuevo = usuarioService.crearUsuarioConRol(usuario);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/{id}/rol/{idRol}")
    public ResponseEntity<Usuario> cambiarRol(@PathVariable Long id, @PathVariable Long idRol) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, idRol));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
