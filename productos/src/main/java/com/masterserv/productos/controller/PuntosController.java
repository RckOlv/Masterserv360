package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CanjePuntosRequestDTO;
import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.SaldoPuntosDTO;
import com.masterserv.productos.service.PuntosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/puntos")
public class PuntosController {

    @Autowired
    private PuntosService puntosService;

    /**
     * Endpoint para que un cliente canjee sus puntos por un cupón.
     */
    @PostMapping("/canjear")
    @PreAuthorize("hasRole('CLIENTE')") // Solo los clientes pueden canjear
    public ResponseEntity<CuponDTO> canjearPuntos(
            @Valid @RequestBody CanjePuntosRequestDTO requestDTO,
            Principal principal) {
        
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String clienteEmail = principal.getName();
        
        try {
            CuponDTO cuponGenerado = puntosService.canjearPuntos(
                clienteEmail, 
                requestDTO.getPuntosACanjear()
            );
            return ResponseEntity.ok(cuponGenerado);
        } catch (RuntimeException e) {
            // Captura errores de negocio (ej. "Saldo insuficiente")
            // (Mejora: Devolver un DTO de error con e.getMessage())
            return ResponseEntity.badRequest().body(null); 
        }
    }

    /**
     * Endpoint para que un cliente (o chatbot) consulte su saldo actual de puntos.
     */
    @GetMapping("/mi-saldo")
    @PreAuthorize("hasRole('CLIENTE')") // ¡Seguridad a nivel de método!
    public ResponseEntity<SaldoPuntosDTO> getMiSaldo(Principal principal) {
        
        // 1. Validar autenticación
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userEmail = principal.getName();
        
        // 2. ¡Llamada real al servicio!
        // (Este método ahora existe en PuntosService)
        SaldoPuntosDTO saldo = puntosService.getSaldoByEmail(userEmail);
        
        // 3. Devolver la respuesta
        return ResponseEntity.ok(saldo);
    }
}