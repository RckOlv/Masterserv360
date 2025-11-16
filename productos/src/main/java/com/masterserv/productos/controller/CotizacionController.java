package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CotizacionAdminDTO;
// (Asegúrate de tener todos los imports necesarios)
import com.masterserv.productos.dto.CotizacionPublicaDTO; 
import com.masterserv.productos.dto.OfertaProveedorDTO;
import com.masterserv.productos.entity.Pedido; // (Importamos Pedido)
import com.masterserv.productos.service.CotizacionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // ¡IMPORTADO!
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
    // --- ¡INICIO DE LA CORRECCIÓN! ---
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarCotizacion( // Cambiado a ResponseEntity<?>
            @PathVariable Long id,
            Principal principal) { // 1. Añadimos Principal
        
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // 2. Pasamos el email del Admin al servicio
            // (Asumimos que el servicio devuelve el Pedido creado)
            Pedido pedidoGenerado = cotizacionService.confirmarCotizacion(id, principal.getName()); 
            
            // 3. Devolvemos el Pedido (o un DTO de Pedido si lo tuviéramos)
            // (El frontend en cotizacion-detalle.ts espera un PedidoDTO)
            // (Por ahora, devolvemos un mensaje de éxito. 
            // ¡Si el frontend falla, lo ajustamos allí!)
            return ResponseEntity.ok(Map.of(
                "message", "Cotización confirmada y Pedido generado.",
                "pedidoId", pedidoGenerado.getId() // Devolvemos el ID del nuevo pedido
            ));
            
        } catch (EntityNotFoundException | IllegalStateException e) {
            // Capturamos errores de negocio (ej. "Cotización no encontrada")
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        // (Los errores 500 (como el de BD) los capturará el GlobalExceptionHandler)
    }
    // --- FIN DE LA CORRECCIÓN ---
}