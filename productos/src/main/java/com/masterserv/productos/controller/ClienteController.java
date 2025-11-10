package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.dto.VentaResumenDTO; // <-- ¡IMPORTAR DTO RESUMEN!
import com.masterserv.productos.service.ClienteService;
import com.masterserv.productos.service.VentaService; // <-- ¡IMPORTAR VENTA SERVICE!
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // <-- ¡IMPORTAR PAGE!
import org.springframework.data.domain.Pageable; // <-- ¡IMPORTAR PAGEABLE!
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // --- ¡INYECTAR VENTA SERVICE! ---
    @Autowired
    private VentaService ventaService;

    /**
     * Endpoint para que el cliente obtenga sus propios datos de perfil.
     */
    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClientePerfilDTO> getMiPerfil(Principal principal) {
        String userEmail = principal.getName();
        ClientePerfilDTO perfil = clienteService.getPerfilByEmail(userEmail);
        return ResponseEntity.ok(perfil);
    }
    
    /**
     * Endpoint para que el cliente actualice sus propios datos de perfil.
     */
    @PutMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClientePerfilDTO> updateMiPerfil(
            @Valid @RequestBody ClientePerfilUpdateDTO updateDTO,
            Principal principal) {
        
        String userEmail = principal.getName();
        ClientePerfilDTO perfilActualizado = clienteService.updatePerfilByEmail(userEmail, updateDTO);
        return ResponseEntity.ok(perfilActualizado);
    }
    
    
    // --- ¡NUEVO ENDPOINT PARA HISTORIAL DE COMPRAS! ---
    
    /**
     * Obtiene el historial de compras (paginado) del cliente autenticado.
     *
     * @param principal El usuario cliente autenticado.
     * @param pageable La configuración de paginación (ej. ?page=0&size=5).
     * @return Una página de VentaResumenDTO.
     */
    @GetMapping("/mis-compras")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<VentaResumenDTO>> getMisCompras(
            Principal principal,
            Pageable pageable) {
        
        String userEmail = principal.getName();
        
        // Llamamos al método que creamos en VentaService
        Page<VentaResumenDTO> historial = ventaService.findVentasByClienteEmail(userEmail, pageable);
        
        return ResponseEntity.ok(historial);
    }
}