package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.SaldoPuntosDTO;
import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.service.PuntosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.masterserv.productos.dto.ClienteFidelidadDTO;
import java.security.Principal;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.service.UsuarioService;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/puntos")
public class PuntosController {

    @Autowired
    private PuntosService puntosService;

    @Autowired
    private com.masterserv.productos.service.UsuarioService usuarioService;

    /**
     * Endpoint para el PORTAL DEL CLIENTE.
     * Obtiene el saldo de puntos actual del cliente logueado.
     * * CORRECCIÓN: Cambiado de "/saldo" a "/mi-saldo" para coincidir con Angular
     */
    @GetMapping("/mi-saldo")
    @PreAuthorize("hasRole('CLIENTE')") 
    public ResponseEntity<SaldoPuntosDTO> getMiSaldo(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build(); 
        }
        String clienteEmail = principal.getName();
        
        SaldoPuntosDTO saldo = puntosService.getSaldoByEmail(clienteEmail);
        return ResponseEntity.ok(saldo);
    }

    /**
     * Endpoint para el PORTAL DEL CLIENTE.
     * Canjea los puntos del cliente logueado por un nuevo cupón (V2).
     * * CORRECCIÓN 1: Cambiado de "/canjear" a "/canje"
     * CORRECCIÓN 2: Cambiado @RequestBody por @RequestParam para recibir el ID simple
     */
    @PostMapping("/canje")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<CuponDTO> canjearMisPuntos(
            @RequestParam Long recompensaId, // <-- Recibimos el ID directo de la URL (?recompensaId=1)
            Principal principal) {
        
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String clienteEmail = principal.getName();

        // Llamamos al servicio pasando el ID de recompensa
        CuponDTO cuponGenerado = puntosService.canjearPuntos(
            clienteEmail, 
            recompensaId
        );
        
        return ResponseEntity.ok(cuponGenerado);
    }

    @GetMapping("/cliente/{clienteId}/fidelidad")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") // Seguridad: Solo staff
    public ResponseEntity<ClienteFidelidadDTO> getInfoFidelidad(@PathVariable Long clienteId) {
        ClienteFidelidadDTO info = puntosService.obtenerInfoFidelidadCliente(clienteId);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/canje-pos")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<CuponDTO> canjearPuntosDesdePos(
            @RequestParam Long clienteId, 
            @RequestParam Long recompensaId) {
        
        UsuarioDTO cliente = usuarioService.findById(clienteId);
        
        // Usamos el email del DTO para el canje
        CuponDTO cupon = puntosService.canjearPuntos(cliente.getEmail(), recompensaId);
        
        return ResponseEntity.ok(cupon);
    }
}