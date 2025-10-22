package com.masterserv.productos.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

import com.masterserv.productos.repository.ProductoRepository;
import com.masterserv.productos.repository.CategoriaRepository;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/estadisticas")
    public Map<String, Object> obtenerEstadisticas() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalProductos", productoRepository.count());
            stats.put("totalCategorias", categoriaRepository.count());
            stats.put("mensaje", "Estadísticas cargadas correctamente");
        } catch (Exception e) {
            stats.put("error", "Error al obtener estadísticas: " + e.getMessage());
        }

        return stats;
    }
}
