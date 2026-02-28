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
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    // ✅ El vendedor ahora puede ver la lista
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ProveedorDTO>> getAllProveedores(@RequestParam(required = false) String estado) {
        List<ProveedorDTO> proveedores = proveedorService.findAll(estado);
        return ResponseEntity.ok(proveedores);
    }

    // ✅ El vendedor ahora puede ver los detalles de un proveedor
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ProveedorDTO> getProveedorById(@PathVariable Long id) {
        ProveedorDTO proveedor = proveedorService.findById(id);
        return ResponseEntity.ok(proveedor);
    }

    // 🔒 Solo el ADMIN puede crear
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorDTO> createProveedor(@Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO nuevoProveedor = proveedorService.create(proveedorDTO);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    // 🔒 Solo el ADMIN puede editar
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProveedorDTO> updateProveedor(@PathVariable Long id, @Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO actualizado = proveedorService.update(id, proveedorDTO);
        return ResponseEntity.ok(actualizado);
    }

    // 🔒 Solo el ADMIN puede borrar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProveedor(@PathVariable Long id) {
        proveedorService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Proveedor marcado como inactivo"));
    }

    // 🔒 Solo el ADMIN puede reactivar
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivarProveedor(@PathVariable Long id) {
        proveedorService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}