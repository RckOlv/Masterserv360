package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ProveedorDTO;
import com.masterserv.productos.service.ProveedorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort; // <--- IMPORTANTE
import org.springframework.data.web.PageableDefault; // <--- IMPORTANTE
import org.springframework.data.domain.Pageable; // <--- IMPORTANTE
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proveedores")
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    /**
     * Lista proveedores (filtrados por estado).
     * NOTA: Este método devuelve List, no Page, así que el orden lo decide el Service
     * o la consulta JPQL. Si quisieras paginarlo a futuro, aquí iría el Pageable.
     * Por ahora lo dejamos igual, pero aseguramos que el Service use un 'Sort' si es necesario.
     */
    @GetMapping
    public ResponseEntity<List<ProveedorDTO>> getAllProveedores(
            @RequestParam(required = false) String estado) {
        // En tu Service deberías asegurarte de que el findAll use un Sort por defecto
        List<ProveedorDTO> proveedores = proveedorService.findAll(estado);
        return ResponseEntity.ok(proveedores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorDTO> getProveedorById(@PathVariable Long id) {
        ProveedorDTO proveedor = proveedorService.findById(id);
        return ResponseEntity.ok(proveedor);
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> createProveedor(@Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO nuevoProveedor = proveedorService.create(proveedorDTO);
        return new ResponseEntity<>(nuevoProveedor, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorDTO> updateProveedor(@PathVariable Long id, @Valid @RequestBody ProveedorDTO proveedorDTO) {
        ProveedorDTO actualizado = proveedorService.update(id, proveedorDTO);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProveedor(@PathVariable Long id) {
        proveedorService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Proveedor marcado como inactivo"));
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivarProveedor(@PathVariable Long id) {
        proveedorService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}