package com.masterserv.productos.controller;

// --- Imports Corregidos ---
// Ya no necesitamos FinalizarVentaDTO
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;         // Para findAll (si lo añades)
import org.springframework.data.domain.Pageable;    // Para findAll (si lo añades)
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
     * Recibe los datos de la venta (cliente, detalles) en el cuerpo.
     * El vendedor se obtiene del usuario autenticado.
     */
    @PostMapping // Cambiado de /finalizar a la raíz /api/ventas para seguir el estándar REST
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    // --- Firma del Método Corregida ---
    // 1. Usamos VentaDTO en lugar de FinalizarVentaDTO
    // 2. Inyectamos Principal para obtener el vendedor
    public ResponseEntity<VentaDTO> crearVenta(@Valid @RequestBody VentaDTO ventaDTO, Principal principal) {

        // Validamos que el usuario esté autenticado
        if (principal == null || principal.getName() == null) {
            // Considera lanzar una excepción o devolver un 401 Unauthorized
             return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName(); // Obtenemos el email del vendedor logueado

        // --- Llamada al Servicio Corregida ---
        // Llamamos al método 'create' que sí existe en VentaService
        VentaDTO ventaCreada = ventaService.create(ventaDTO, vendedorEmail);

        return new ResponseEntity<>(ventaCreada, HttpStatus.CREATED);
    }

    // --- Otros Endpoints (Ejemplos para añadir después) ---

    /**
     * Obtiene una venta por su ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> getVentaById(@PathVariable Long id) {
        VentaDTO venta = ventaService.findById(id);
        return ResponseEntity.ok(venta);
    }

    /**
     * Obtiene todas las ventas de forma paginada.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> getAllVentas(Pageable pageable) {
        Page<VentaDTO> ventas = ventaService.findAll(pageable);
        return ResponseEntity.ok(ventas);
    }

    /**
     * Cancela una venta (y repone el stock).
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')") // Solo Admin puede cancelar? Ajusta roles si es necesario
    public ResponseEntity<Void> cancelarVenta(@PathVariable Long id, Principal principal) {
         if (principal == null || principal.getName() == null) {
             return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
         }
         String usuarioEmailCancela = principal.getName();
         ventaService.cancelarVenta(id, usuarioEmailCancela);
         return ResponseEntity.noContent().build(); // 204 No Content
    }

}