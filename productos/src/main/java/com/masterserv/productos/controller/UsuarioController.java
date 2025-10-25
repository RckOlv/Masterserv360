package com.masterserv.productos.controller;

import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO; // Importado
import com.masterserv.productos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // Importado
import org.springframework.data.domain.Pageable; // Importado
import org.springframework.data.web.PageableDefault; // Importado
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List; // Se quita si no se usa
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')") // Protegemos todo el controlador
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * MODIFICADO: Cambia de GET a POST /filtrar para recibir filtros
     * y devolver datos paginados.
     */
    @PostMapping("/filtrar")
    public ResponseEntity<Page<UsuarioDTO>> listarFiltrado(
            @RequestBody UsuarioFiltroDTO filtro,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(filtro, pageable));
    }

    /**
     * Obtiene los detalles de un usuario por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getUsuarioById(@PathVariable Long id) {
        UsuarioDTO usuario = usuarioService.findById(id); 
        return ResponseEntity.ok(usuario);
    }
    
    /**
     * Crea un nuevo usuario (función de Admin).
     */
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoUsuario = usuarioService.crearUsuarioAdmin(usuarioDTO);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    /**
     * Actualiza un usuario existente (función de Admin).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO usuarioActualizado = usuarioService.actualizarUsuarioAdmin(id, usuarioDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    /**
     * Realiza un borrado lógico (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> softDeleteUsuario(@PathVariable Long id) {
        usuarioService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Usuario marcado como inactivo"));
    }

    /**
     * Reactiva un usuario inactivo.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarUsuario(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}