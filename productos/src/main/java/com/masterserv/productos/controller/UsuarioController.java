package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CambioPasswordDTO;
import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.dto.UsuarioFiltroDTO;
import com.masterserv.productos.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <--- IMPORTADO
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/perfil")
    public ResponseEntity<UsuarioDTO> getMiPerfil(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(usuarioService.buscarPorEmail(email)); 
    }

    @PutMapping("/perfil")
    public ResponseEntity<UsuarioDTO> updateMiPerfil(@Valid @RequestBody UsuarioDTO dto, Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(usuarioService.actualizarMiPerfil(email, dto));
    }

    @PatchMapping("/perfil/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(@Valid @RequestBody CambioPasswordDTO dto, Principal principal) {
        String email = principal.getName();
        usuarioService.cambiarPassword(email, dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/filtrar")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<Page<UsuarioDTO>> listarFiltrado(
            @RequestBody UsuarioFiltroDTO filtro,
            // ✅ ORDENADO POR APELLIDO A-Z
            @PageableDefault(page = 0, size = 10, sort = "apellido", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.filtrarUsuarios(filtro, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<UsuarioDTO> getUsuarioById(@PathVariable Long id) {
        UsuarioDTO usuario = usuarioService.findById(id);
        return ResponseEntity.ok(usuario);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> crearUsuario(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoUsuario = usuarioService.crearUsuarioAdmin(usuarioDTO);
        return new ResponseEntity<>(nuevoUsuario, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioDTO> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO usuarioActualizado = usuarioService.actualizarUsuarioAdmin(id, usuarioDTO);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> softDeleteUsuario(@PathVariable Long id) {
        usuarioService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Usuario marcado como inactivo"));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivarUsuario(@PathVariable Long id) {
        usuarioService.reactivar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cliente-rapido")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") 
    public ResponseEntity<UsuarioDTO> registrarClienteRapido(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        UsuarioDTO nuevoCliente = usuarioService.crearClienteRapido(usuarioDTO);
        return new ResponseEntity<>(nuevoCliente, HttpStatus.CREATED);
    }
}