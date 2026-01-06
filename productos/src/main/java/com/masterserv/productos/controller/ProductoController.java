package com.masterserv.productos.controller;

import com.masterserv.productos.dto.MovimientoStockDTO;
import com.masterserv.productos.dto.ProductoDTO;
import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.service.MovimientoStockService; // ✅ IMPORTANTE
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

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private MovimientoStockService movimientoStockService; // ✅ INYECTAMOS EL SERVICIO CORRECTO

    // --- MENTOR: ENDPOINT PARA GENERAR CÓDIGO ---
    @GetMapping("/generar-codigo")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Map<String, String>> generarCodigo(
            @RequestParam Long categoriaId, 
            @RequestParam String nombre) {
        
        String nuevoCodigo = productoService.generarCodigoAutomatico(categoriaId, nombre);
        return ResponseEntity.ok(Map.of("codigo", nuevoCodigo));
    }
    // --------------------------------------------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<ProductoDTO>> getAllProductos(
            @PageableDefault(page = 0, size = 10, sort = "nombre") Pageable pageable) {
        Page<ProductoDTO> productos = productoService.findAll(pageable);
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/filtrar")
    public ResponseEntity<Page<ProductoDTO>> filterProductos(
            @RequestBody ProductoFiltroDTO filtro,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        
        Page<ProductoDTO> productoPage = productoService.filter(filtro, pageable);
        return ResponseEntity.ok(productoPage);
    }

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
            @RequestParam(defaultValue = "") String search,
            Pageable pageable
    ) {
        Page<ProductoDTO> pagina = productoService.searchByProveedor(proveedorId, search, pageable);
        return ResponseEntity.ok(pagina);
    }

    @PostMapping("/ajuste-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Void> realizarAjusteStock(@RequestBody @Valid MovimientoStockDTO dto) {
        
        System.out.println(">>> [CONTROLLER] Redirigiendo a MovimientoStockService...");
        
        // Usamos el servicio especialista que tiene la lógica de auditoría manual
        movimientoStockService.registrarMovimiento(dto);
        
        return ResponseEntity.ok().build();
    }
}