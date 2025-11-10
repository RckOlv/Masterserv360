package com.masterserv.productos.controller;

import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO; // <-- Importar DTO Filtro
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // ¡Importante para obtener el vendedor!

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    /**
     * Endpoint principal para crear (finalizar) una nueva venta.
     */
    @PostMapping // Correcto: POST a /api/ventas
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> crearVenta(@Valid @RequestBody VentaDTO ventaDTO, Principal principal) {
        if (principal == null || principal.getName() == null) {
             return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        VentaDTO ventaCreada = ventaService.create(ventaDTO, vendedorEmail);
        return new ResponseEntity<>(ventaCreada, HttpStatus.CREATED);
    }

    /**
     * Obtiene una venta por su ID (con detalles).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> getVentaById(@PathVariable Long id) {
        VentaDTO venta = ventaService.findById(id); // Usa findByIdWithDetails internamente
        return ResponseEntity.ok(venta);
    }

    /**
     * Obtiene todas las ventas de forma paginada (SIN filtros).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> getAllVentas(Pageable pageable) {
        Page<VentaDTO> ventas = ventaService.findAll(pageable); // Llama al método sin filtros
        return ResponseEntity.ok(ventas);
    }

    // --- ¡NUEVO ENDPOINT PARA FILTRAR! ---
    /**
     * Obtiene las ventas filtradas según criterios y paginación.
     * Recibe los filtros en el cuerpo de la petición (POST).
     *
     * @param filtro DTO con los criterios de búsqueda (clienteId, vendedorId, fechaDesde, fechaHasta, estado).
     * @param pageable Objeto Pageable inyectado por Spring (desde query params ?page=..&size=..&sort=..)
     * @return ResponseEntity con la página de VentaDTO filtrada.
     */
    @PostMapping("/filtrar") // Usamos POST para enviar el cuerpo del filtro
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> findVentasByCriteria(
            @RequestBody VentaFiltroDTO filtro, Pageable pageable) {
        // Llama al nuevo método del servicio que usa Specifications
        Page<VentaDTO> ventasFiltradas = ventaService.findByCriteria(filtro, pageable);
        return ResponseEntity.ok(ventasFiltradas);
    }
    // ------------------------------------


    /**
     * Cancela una venta (y repone el stock).
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')") // Ajusta roles si es necesario
    public ResponseEntity<Void> cancelarVenta(@PathVariable Long id, Principal principal) {
         if (principal == null || principal.getName() == null) {
              return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
         }
         String usuarioEmailCancela = principal.getName();
         ventaService.cancelarVenta(id, usuarioEmailCancela);
         return ResponseEntity.noContent().build(); // 204 No Content
    }

}