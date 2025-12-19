package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ProveedorDTO;
import com.masterserv.productos.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proveedores")
@PreAuthorize("hasRole('ADMIN')") // Todo el módulo es solo para Admin
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    /**
     * Lista proveedores (filtrados por estado: ACTIVO, INACTIVO, TODOS).
     */
    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> getAllProveedores(
            @RequestParam(required = false) String estado) {
        List<ProveedorDTO> proveedores = proveedorService.findAll(estado);
        return ResponseEntity.ok(proveedores);
    }

    /**
     * Obtiene un proveedor por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> getProveedorById(@PathVariable Long id) {
        ProveedorDTO proveedor = proveedorService.findById(id);
        return ResponseEntity.ok(proveedor);
    }

    /**
     * Crea un nuevo proveedor.
     */
    @PostMapping
    public ResponseEntity<ProveedorDTO> createProveedor(@Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO nuevoProveedor = proveedorService.create(proveedorDTO);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    /**
     * Actualiza un proveedor existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> updateProveedor(@PathVariable Long id, @Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO actualizado = proveedorService.update(id, proveedorDTO);
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Desactiva (Soft Delete) un proveedor.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProveedor(@PathVariable Long id) {
        proveedorService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Proveedor marcado como inactivo"));
    }

    /**
     * Reactiva un proveedor inactivo.
     */
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarProveedor(@PathVariable Long id) {
        proveedorService.reactivar(id);
        return ResponseEntity.noContent().build(); // 204 No Content (Éxito)
    }
}