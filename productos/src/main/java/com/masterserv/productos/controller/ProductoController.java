package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /** 🔹 Listar productos activos */
    @GetMapping
    public ResponseEntity<List<Producto>> listarActivos() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    /** 🔹 Listar todos los productos (activos e inactivos) */
    @GetMapping("/todos")
    public ResponseEntity<List<Producto>> listarTodos() {
        return ResponseEntity.ok(productoService.listarProductos());
    }

    /** 🔹 Crear producto con validación */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Producto producto) {
        try {
            Producto nuevo = productoService.creaProducto(producto);
            return ResponseEntity.ok(nuevo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** 🔹 Actualizar producto existente */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Producto producto) {
        try {
            producto.setIdProducto(id);
            Producto actualizado = productoService.actualizarProducto(producto);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 🔹 Filtrado combinado flexible (con conversión de fechas String → LocalDate)
     */
     @PostMapping("/filtrar")
    public List<Producto> filtrarProductos(@RequestBody ProductoFiltroDTO filtro) {
        System.out.println("Nombre recibido desde DTO: " + filtro.getNombre());
        return productoService.filtrarProductos(filtro);
    }

    /** 🔹 Inactivar producto */
    @PutMapping("/{id}/inactivar")
    public ResponseEntity<Map<String, String>> inactivarProducto(@PathVariable Long id) {
        productoService.cambiarActivo(id, false);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Producto inactivado correctamente");
        return ResponseEntity.ok(response);
    }

    /** 🔹 Reactivar producto */
    @PutMapping("/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivarProducto(@PathVariable Long id) {
        productoService.cambiarActivo(id, true);
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "Producto reactivado correctamente");
        return ResponseEntity.ok(response);
    }

    /** 🔹 Filtrar solo por fechas */
    @GetMapping("/filtrar-fecha")
    public ResponseEntity<List<Producto>> filtrarPorFecha(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {

        LocalDate fechaDesde = (desde != null && !desde.isEmpty()) ? LocalDate.parse(desde) : null;
        LocalDate fechaHasta = (hasta != null && !hasta.isEmpty()) ? LocalDate.parse(hasta) : null;

        List<Producto> productos = productoService.filtrarPorFechas(fechaDesde, fechaHasta);
        return ResponseEntity.ok(productos);
    }

    /** 🔹 Últimos 5 productos */
    @GetMapping("/ultimos")
    public ResponseEntity<List<Producto>> obtenerUltimosProductos() {
        return ResponseEntity.ok(productoService.obtenerUltimosProductos());
    }
}
