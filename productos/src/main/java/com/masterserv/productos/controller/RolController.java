package com.masterserv.productos.controller;

import com.masterserv.productos.dto.RolDTO;
import com.masterserv.productos.service.RolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolService rolService;

    /**
     * Obtiene la lista de roles (Usado en formularios y listados).
     * NOTA: Permitimos a todos los usuarios autenticados leer los roles.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()") // Cualquiera logueado puede ver los roles
    public ResponseEntity<List<RolDTO>> getAllRoles() {
        return ResponseEntity.ok(rolService.findAll());
    }

    /**
     * Crea un nuevo rol.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Solo el ADMIN puede crear roles
    public ResponseEntity<RolDTO> createRol(@Valid @RequestBody RolDTO rolDTO) {
        RolDTO nuevoRol = rolService.create(rolDTO);
        return new ResponseEntity<>(nuevoRol, HttpStatus.CREATED);
    }

    /**
     * Actualiza un rol existente.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Solo el ADMIN puede editar
    public ResponseEntity<RolDTO> updateRol(@PathVariable Long id, @Valid @RequestBody RolDTO rolDTO) {
        RolDTO rolActualizado = rolService.update(id, rolDTO);
        return ResponseEntity.ok(rolActualizado);
    }

    /**
     * Elimina un rol.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Solo el ADMIN puede eliminar
    public ResponseEntity<Map<String, String>> deleteRol(@PathVariable Long id) {
        rolService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Rol eliminado exitosamente"));
    }
}