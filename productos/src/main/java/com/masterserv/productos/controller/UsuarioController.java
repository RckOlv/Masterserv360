package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CambioPasswordDTO; // Importar DTO
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

import java.security.Principal; // Importar Principal
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // --- MENTOR: NUEVOS ENDPOINTS PARA PERFIL PROPIO (Admin/Vendedor) ---
    // IMPORTANTE: Estos deben ir ANTES de @GetMapping("/{id}") para evitar conflictos.

    /**
     * Obtener MI perfil (para el usuario logueado en el POS).
     */
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioDTO> getMiPerfil(Principal principal) {
        String email = principal.getName();
        // Requiere que hayas agregado 'buscarPorEmail' en UsuarioService
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email)); 
    }

    /**
     * Actualizar MI perfil.
     */
    @PutMapping("/perfil")
    public ResponseEntity<UsuarioDTO> updateMiPerfil(@Valid @RequestBody UsuarioDTO dto, Principal principal) {
        String email = principal.getName();
        // Requiere 'actualizarMiPerfil' en UsuarioService
        return ResponseEntity.ok(usuarioService.actualizarMiPerfil(email, dto));
    }

    /**
     * Cambiar MI contraseña.
     */
    @PatchMapping("/perfil/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambioPasswordDTO dto, Principal principal) {
        String email = principal.getName();
        // Requiere 'cambiarPassword' en UsuarioService
        usuarioService.cambiarPassword(email, dto);
        return ResponseEntity.noContent().build();
    }
    // --------------------------------------------------------------------

    /**
     * Filtra y pagina usuarios.
     */
    @PostMapping("/filtrar")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<Page<UsuarioDTO>> listarFiltrado(
            @RequestBody UsuarioFiltroDTO filtro,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(filtro, pageable));
    }

    /**
     * Obtiene los detalles de un usuario por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<UsuarioDTO> getUsuarioById(@PathVariable Long id) {
        UsuarioDTO usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }

    /**
     * Crea un nuevo usuario. SOLO ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoUsuario = usuarioService.crearUsuarioAdmin(usuarioDTO);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    /**
     * Actualiza un usuario existente. SOLO ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO usuarioActualizado = usuarioService.actualizarUsuarioAdmin(id, usuarioDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    /**
     * Realiza un borrado lógico (soft delete). SOLO ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> softDeleteUsuario(@PathVariable Long id) {
        usuarioService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Usuario marcado como inactivo"));
    }

    /**
     * Reactiva un usuario inactivo. SOLO ADMIN.
     */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivarUsuario(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint específico para POS (Punto de Venta).
     */
    @PostMapping("/cliente-rapido")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") 
    public ResponseEntity<UsuarioDTO> registrarClienteRapido(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoCliente = usuarioService.crearClienteRapido(usuarioDTO);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }
}