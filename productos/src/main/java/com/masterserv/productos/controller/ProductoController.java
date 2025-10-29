package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
// @CrossOrigin(origins = "http://localhost:4200") // No es necesario, lo pusimos global en SecurityConfig
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    // --- Endpoint de Filtrado (Público o Semi-público) ---
    // Este POST es para la búsqueda. Lo hacemos POST para poder enviar un body (el DTO de filtro).
    @PostMapping("/filtrar")
    public ResponseEntity<Page<ProductoDTO>> filterProductos(
            @RequestBody ProductoFiltroDTO filtro,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        
        Page<ProductoDTO> productoPage = productoService.filter(filtro, pageable);
        return ResponseEntity.ok(productoPage);
    }

    // --- Endpoints CRUD (Protegidos) ---

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ProductoDTO> getProductoById(@PathVariable Long id) {
        ProductoDTO producto = productoService.findById(id);
        return ResponseEntity.ok(producto);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoDTO> createProducto(@Valid @RequestBody ProductoDTO productoDTO) {
        ProductoDTO nuevoProducto = productoService.create(productoDTO);
        return new ResponseEntity<>(nuevoProducto, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoDTO> updateProducto(@PathVariable Long id, @Valid @RequestBody ProductoDTO productoDTO) {
        ProductoDTO productoActualizado = productoService.update(id, productoDTO);
        return ResponseEntity.ok(productoActualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteProducto(@PathVariable Long id) {
        // Llamamos al nuevo método
        productoService.softDelete(id); 
        return ResponseEntity.ok(Map.of("message", "Producto marcado como inactivo exitosamente"));
    }

    @GetMapping("/por-proveedor/{proveedorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<List<ProductoDTO>> getProductosByProveedor(@PathVariable Long proveedorId) {
        return ResponseEntity.ok(productoService.findByProveedorId(proveedorId));
    }

    @GetMapping("/search-by-proveedor")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<ProductoDTO>> searchByProveedor(
            @RequestParam Long proveedorId,
            @RequestParam(defaultValue = "") String search, // El término de búsqueda
            Pageable pageable // Spring arma el page, size, sort
    ) {
        Page<ProductoDTO> pagina = productoService.searchByProveedor(proveedorId, search, pageable);
        return ResponseEntity.ok(pagina);
    }
}