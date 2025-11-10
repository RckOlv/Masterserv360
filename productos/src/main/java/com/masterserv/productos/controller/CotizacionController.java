package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CotizacionAdminDTO;
import com.masterserv.productos.service.CotizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cotizaciones")
@PreAuthorize("hasRole('ADMIN')") // ¡TODA la clase protegida para ADMIN!
public class CotizacionController {

    @Autowired
    private CotizacionService cotizacionService;

    /**
     * Busca todas las cotizaciones que están 'RECIBIDAS'
     * (listas para que el Admin las revise).
     */
    @GetMapping("/recibidas")
    public ResponseEntity<List<CotizacionAdminDTO>> getCotizacionesRecibidas() {
        List<CotizacionAdminDTO> cotizaciones = cotizacionService.findCotizacionesRecibidas();
        return ResponseEntity.ok(cotizaciones);
    }

    /**
     * Obtiene el detalle completo de UNA cotización por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CotizacionAdminDTO> getCotizacionById(@PathVariable Long id) {
        CotizacionAdminDTO cotizacion = cotizacionService.findCotizacionAdminById(id);
        return ResponseEntity.ok(cotizacion);
    }

    /**
     * ¡ACCIÓN! Cancela un ITEM específico de una cotización.
     * (Cumple tu requisito de cancelación parcial).
     */
    @PatchMapping("/item/{itemId}/cancelar")
    public ResponseEntity<Map<String, String>> cancelarItem(@PathVariable Long itemId) {
        cotizacionService.cancelarItem(itemId);
        return ResponseEntity.ok(Map.of("message", "Item cancelado correctamente."));
    }
    
    /**
     * ¡ACCIÓN! Cancela una COTIZACIÓN completa.
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Map<String, String>> cancelarCotizacion(@PathVariable Long id) {
        cotizacionService.cancelarCotizacion(id);
        return ResponseEntity.ok(Map.of("message", "Cotización cancelada correctamente."));
    }
    
    /**
     * ¡ACCIÓN FINAL! Confirma una cotización.
     * Esto la convierte en un Pedido real y rechaza las otras.
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Map<String, String>> confirmarCotizacion(@PathVariable Long id) {
        // (Devolveremos el Pedido creado en el futuro, por ahora un mensaje)
        cotizacionService.confirmarCotizacion(id);
        return ResponseEntity.ok(Map.of("message", "Cotización confirmada y Pedido generado."));
    }
}