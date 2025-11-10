package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CotizacionPublicaDTO;
import com.masterserv.productos.dto.OfertaProveedorDTO;
import com.masterserv.productos.service.CotizacionService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador para endpoints PÚBLICOS que no requieren autenticación JWT,
 * como el portal de ofertas para proveedores.
 */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    private CotizacionService cotizacionService;

    /**
     * Endpoint GET para que el proveedor VEA la solicitud.
     */
    @GetMapping("/oferta/{token}")
    public ResponseEntity<?> getOfertaPorToken(@PathVariable String token) {
        try {
            CotizacionPublicaDTO cotizacionDTO = cotizacionService.findCotizacionPublicaByToken(token);
            return ResponseEntity.ok(cotizacionDTO);
        
        } catch (EntityNotFoundException e) {
            // 404 Not Found
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", e.getMessage()));
        
        } catch (IllegalStateException e) {
            // 410 Gone (El token ya fue usado o expiró)
            return ResponseEntity
                .status(HttpStatus.GONE)
                .body(Map.of("status", "error", "message", e.getMessage()));
        
        } catch (Exception e) {
            // 500 Internal Server Error
            // (Nuestro GlobalExceptionHandler también lo capturará)
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error interno al procesar la solicitud."));
        }
    }

    /**
     * Endpoint POST para que el proveedor ENVÍE su oferta.
     */
    @PostMapping("/oferta/{token}")
    public ResponseEntity<?> submitOfertaProveedor(
            @PathVariable String token,
            @Valid @RequestBody OfertaProveedorDTO ofertaDTO) {
        
        try {
            cotizacionService.submitOfertaProveedor(token, ofertaDTO);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Oferta recibida exitosamente."));

        } catch (EntityNotFoundException e) {
            // 404 Not Found
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", "error", "message", e.getMessage()));
        
        } catch (IllegalStateException e) {
             // 410 Gone (El token ya fue usado)
             return ResponseEntity
                .status(HttpStatus.GONE)
                .body(Map.of("status", "error", "message", e.getMessage()));
        
        } catch (SecurityException e) {
             // 403 Forbidden (Intentó cotizar un item de otra solicitud)
             return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", "error", "message", e.getMessage()));
        
        } catch (Exception e) {
            // 500 Internal Server Error
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", "Error interno al procesar la solicitud."));
        }
    }
}