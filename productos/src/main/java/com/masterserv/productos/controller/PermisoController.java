package com.masterserv.productos.controller;

import com.masterserv.productos.dto.PermisoDTO;
import com.masterserv.productos.service.PermisoService;
import jakarta.validation.Valid; // Mentor: Importar Valid
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // Mentor: Importar HttpStatus
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*; // Mentor: Importar *

import java.util.List;

@RestController
@RequestMapping("/permisos") 
@PreAuthorize("hasAuthority('ROLES_MANAGE')") // Proteger toda la clase
public class PermisoController {

    @Autowired
    private PermisoService permisoService;

    @GetMapping
    public ResponseEntity<List<PermisoDTO>> getAllPermisos() {
        List<PermisoDTO> permisos = permisoService.listarPermisos();
        return ResponseEntity.ok(permisos);
    }

    // --- Mentor: ENDPOINT NUEVO (POST) ---
    @PostMapping
    public ResponseEntity<PermisoDTO> createPermiso(@Valid @RequestBody PermisoDTO permisoDTO) {
        PermisoDTO nuevoPermiso = permisoService.crear(permisoDTO);
        return new ResponseEntity<>(nuevoPermiso, HttpStatus.CREATED);
    }

    // --- Mentor: ENDPOINT NUEVO (PUT) ---
    @PutMapping("/{id}")
    public ResponseEntity<PermisoDTO> updatePermiso(@PathVariable Long id, @Valid @RequestBody PermisoDTO permisoDTO) {
        // Asegurarse de que el ID del DTO coincida con el ID de la URL
        permisoDTO.setId(id); 
        PermisoDTO actualizado = permisoService.actualizar(permisoDTO);
        return ResponseEntity.ok(actualizado);
    }

    // --- Mentor: ENDPOINT NUEVO (DELETE) ---
    // (Tu frontend llamó a softDelete, pero el servicio usa delete físico)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermiso(@PathVariable Long id) {
        permisoService.softDelete(id); // O 'delete(id)' si no tienes estado
        return ResponseEntity.noContent().build();
    }
}