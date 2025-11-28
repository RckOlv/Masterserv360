package com.masterserv.productos.controller;

import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO;
import com.masterserv.productos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
// Quitar import no usado: import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
// --- ¡QUITAR @PreAuthorize DE AQUÍ! ---
// @PreAuthorize("hasRole('ADMIN')") // Ya no protege toda la clase
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Filtra y pagina usuarios.
     * Permitido para cualquier usuario autenticado (ADMIN o VENDEDOR).
     */
    @PostMapping("/filtrar")
    // --- AÑADIR @PreAuthorize AQUÍ (más permisivo) ---
    @PreAuthorize("isAuthenticated()") // Permite a cualquier logueado filtrar
    public ResponseEntity<Page<UsuarioDTO>> listarFiltrado(
            @RequestBody UsuarioFiltroDTO filtro,
            @PageableDefault(page = 0, size = 10) Pageable pageable) { // @PageableDefault es opcional si ya configuras en frontend

        // Asegurarse que el servicio tenga el método correcto
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(filtro, pageable));
    }

    /**
     * Obtiene los detalles de un usuario por ID.
     * Permitido para cualquier usuario autenticado.
     */
    @GetMapping("/{id}")
    // --- AÑADIR @PreAuthorize AQUÍ ---
    @PreAuthorize("isAuthenticated()") // Permite ver detalle si estás logueado
    public ResponseEntity<UsuarioDTO> getUsuarioById(@PathVariable Long id) {
        // Asegurarse que el servicio tenga findById
        UsuarioDTO usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Crea un nuevo usuario. SOLO ADMIN.
     */
    @PostMapping
    // --- AÑADIR @PreAuthorize AQUÍ (restringido) ---
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoUsuario = usuarioService.crearUsuarioAdmin(usuarioDTO);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    /**
     * Actualiza un usuario existente. SOLO ADMIN.
     */
    @PutMapping("/{id}")
    // --- AÑADIR @PreAuthorize AQUÍ (restringido) ---
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO usuarioActualizado = usuarioService.actualizarUsuarioAdmin(id, usuarioDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    /**
     * Realiza un borrado lógico (soft delete). SOLO ADMIN.
     */
    @DeleteMapping("/{id}")
    // --- AÑADIR @PreAuthorize AQUÍ (restringido) ---
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> softDeleteUsuario(@PathVariable Long id) {
        usuarioService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Usuario marcado como inactivo"));
    }

    /**
     * Reactiva un usuario inactivo. SOLO ADMIN.
     */
    @PatchMapping("/{id}/reactivar")
    // --- AÑADIR @PreAuthorize AQUÍ (restringido) ---
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivarUsuario(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint específico para POS (Punto de Venta).
     * Permite a VENDEDORES y ADMINS registrar clientes rápidamente.
     */
    @PostMapping("/cliente-rapido")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") // <--- ¡AQUÍ ESTÁ LA MAGIA!
    public ResponseEntity<UsuarioDTO> registrarClienteRapido(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoCliente = usuarioService.crearClienteRapido(usuarioDTO);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }
}