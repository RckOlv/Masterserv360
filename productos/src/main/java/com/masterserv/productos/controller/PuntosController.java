package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CanjePuntosRequestDTO;
import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.SaldoPuntosDTO;
import com.masterserv.productos.service.PuntosService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
     * Endpoint para el PORTAL DEL CLIENTE.
     * Obtiene el saldo de puntos actual del cliente logueado.
     */
    @GetMapping("/saldo")
    @PreAuthorize("hasRole('CLIENTE')") // ¡Solo un CLIENTE puede ver su saldo!
    public ResponseEntity<SaldoPuntosDTO> getMiSaldo(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build(); // No autorizado
        }
        String clienteEmail = principal.getName();
        
        SaldoPuntosDTO saldo = puntosService.getSaldoByEmail(clienteEmail);
        return ResponseEntity.ok(saldo);
    }

    /**
     * Endpoint para el PORTAL DEL CLIENTE.
     * Canjea los puntos del cliente logueado por un nuevo cupón.
     */
    @PostMapping("/canjear")
    @PreAuthorize("hasRole('CLIENTE')") // ¡Solo un CLIENTE puede canjear sus puntos!
    public ResponseEntity<CuponDTO> canjearMisPuntos(
            @Valid @RequestBody CanjePuntosRequestDTO canjeRequest,
            Principal principal) {
        
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build(); // No autorizado
        }
        String clienteEmail = principal.getName();

        // El PuntosService ya maneja las excepciones (saldo insuficiente, etc.)
        // que nuestro GlobalExceptionHandler convertirá en un error 400.
        CuponDTO cuponGenerado = puntosService.canjearPuntos(
            clienteEmail, 
            canjeRequest.getPuntosACanjear()
        );
        
        return ResponseEntity.ok(cuponGenerado);
    }

    // (Aquí irían otros endpoints que necesites, ej. para el Admin)
}