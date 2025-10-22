package com.masterserv.productos.controller;

import com.masterserv.productos.entity.Categoria;
import com.masterserv.productos.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorias")
@CrossOrigin(origins = "http://localhost:4200")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    // 🔸 Listar todas las categorías activas
    @GetMapping
    public ResponseEntity<List<Categoria>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.listarActivas());
    }

    // 🔸 Buscar una categoría por ID
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerCategoria(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔸 Crear nueva categoría
    @PostMapping
    public ResponseEntity<Categoria> crearCategoria(@RequestBody Categoria categoria) {
        Categoria nueva = categoriaService.crearCategoria(categoria);
        return ResponseEntity.ok(nueva);
    }

    // 🔸 Actualizar categoría
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @RequestBody Categoria categoria) {
        return categoriaService.actualizar(id, categoria)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 🔸 Eliminar categoría (lógico o físico según tenga productos)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarCategoria(@PathVariable Long id) {
        categoriaService.eliminarCategoria(id);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Categoría eliminada correctamente");
        return ResponseEntity.ok(response);
    }

    // 🔸 Reactivar categoría
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivar(@PathVariable Long id) {
        categoriaService.cambiarEstado(id, true);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Categoría reactivada");
        return ResponseEntity.ok(response);
    }

    // 🔸 Inactivar categoría
    @PutMapping("/{id}/inactivar")
    public ResponseEntity<Map<String, String>> inactivar(@PathVariable Long id) {
        categoriaService.cambiarEstado(id, false);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Categoría inactivada");
        return ResponseEntity.ok(response);
    }
}
