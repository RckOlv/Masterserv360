package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CategoriaDTO;
import com.masterserv.productos.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // --- ¡MÉTODO CORREGIDO! ---
    // Añadimos @RequestParam para recibir el filtro de estado
    @GetMapping
    public ResponseEntity<List<CategoriaDTO>> getAllCategorias(
            @RequestParam(required = false) String estado) {
        
        List<CategoriaDTO> categorias = categoriaService.findAll(estado);
        return ResponseEntity.ok(categorias);
    }
    // -------------------------

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<CategoriaDTO> getCategoriaById(@PathVariable Long id) {
        CategoriaDTO categoria = categoriaService.findById(id);
        return ResponseEntity.ok(categoria);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaDTO> createCategoria(@Valid @RequestBody CategoriaDTO categoriaDTO) {
        CategoriaDTO nuevaCategoria = categoriaService.create(categoriaDTO);
        return new ResponseEntity<>(nuevaCategoria, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaDTO> updateCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaDTO categoriaDTO) {
        CategoriaDTO categoriaActualizada = categoriaService.update(id, categoriaDTO);
        return ResponseEntity.ok(categoriaActualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCategoria(@PathVariable Long id) {
        categoriaService.softDelete(id); 
        return ResponseEntity.ok(Map.of("message", "Categoría marcada como inactiva"));
    }

    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reactivarCategoria(@PathVariable Long id) {
        categoriaService.reactivar(id);
        return ResponseEntity.noContent().build();
    }
}