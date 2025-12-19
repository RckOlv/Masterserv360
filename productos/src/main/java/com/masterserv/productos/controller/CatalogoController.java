package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ProductoPublicoDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO; // <-- ¡IMPORTAR DTO DE FILTRO!
import com.masterserv.productos.service.ProductoService; // <-- ¡IMPORTAR SERVICIO!
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/catalogo")
public class CatalogoController {

    // --- ¡CORRECCIÓN 1: INYECTAR EL SERVICIO! ---
    @Autowired
    private ProductoService productoService; 
    // ------------------------------------------

    /**
     * Endpoint PÚBLICO para obtener el catálogo de productos paginado.
     */
    @GetMapping("/productos")
    @PreAuthorize("permitAll()") // ¡Este endpoint es público!
    public ResponseEntity<Page<ProductoPublicoDTO>> getCatalogo(Pageable pageable) {
        
        // --- ¡CORRECCIÓN 2: LLAMAR AL SERVICIO REAL! ---
        // (Quitamos el placeholder 'Page.empty()')
        Page<ProductoPublicoDTO> catalogo = productoService.findAllPublico(pageable);
        return ResponseEntity.ok(catalogo);
        // -----------------------------------------------
    }
    
    /**
     * Endpoint PÚBLICO para FILTRAR el catálogo de productos.
     */
    @PostMapping("/productos/filtrar")
    @PreAuthorize("permitAll()") // ¡Este endpoint también es público!
    public ResponseEntity<Page<ProductoPublicoDTO>> filtrarCatalogo(
            @RequestBody ProductoPublicoFiltroDTO filtro, 
            Pageable pageable) {
        
        // Llamamos al método de filtrado público que ya creamos
        Page<ProductoPublicoDTO> catalogo = productoService.findPublicoByCriteria(filtro, pageable);
        return ResponseEntity.ok(catalogo);
    }
}