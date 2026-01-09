package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CambioPasswordDTO;
import com.masterserv.productos.dto.ClienteDTO; // <--- Importar
import com.masterserv.productos.dto.ClientePerfilDTO;
import com.masterserv.productos.dto.ClientePerfilUpdateDTO;
import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.dto.VentaResumenDTO;
import com.masterserv.productos.entity.Usuario; // <--- Importar
import com.masterserv.productos.service.ClienteService;
import com.masterserv.productos.service.CuponService;
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/cliente")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CuponService cuponService;

    // --- ENDPOINTS DE PERFIL (Cliente) ---

    @GetMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClientePerfilDTO> getMiPerfil(Principal principal) {
        String userEmail = principal.getName();
        ClientePerfilDTO perfil = clienteService.getPerfilByEmail(userEmail);
        return ResponseEntity.ok(perfil);
    }
    
    @PutMapping("/mi-perfil")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ClientePerfilDTO> updateMiPerfil(
            @Valid @RequestBody ClientePerfilUpdateDTO updateDTO,
            Principal principal) {
        
        String userEmail = principal.getName();
        ClientePerfilDTO perfilActualizado = clienteService.updatePerfilByEmail(userEmail, updateDTO);
        return ResponseEntity.ok(perfilActualizado);
    }
    
    @PatchMapping("/cambiar-password")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambioPasswordDTO dto,
            Principal principal) {
        
        String userEmail = principal.getName();
        clienteService.cambiarPassword(userEmail, dto);
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mis-compras")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<VentaResumenDTO>> getMisCompras(
            Principal principal,
            Pageable pageable) {
        
        String userEmail = principal.getName();
        Page<VentaResumenDTO> historial = ventaService.findVentasByClienteEmail(userEmail, pageable);
        return ResponseEntity.ok(historial);
    }

    // --- NUEVO ENDPOINT: REGISTRO DESDE POS (Admin/Vendedor) ---
    @PostMapping("/registro-pos")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Usuario> registrarDesdePos(@RequestBody ClienteDTO dto) {
        // Llamamos al servicio nuevo
        Usuario nuevoUsuario = clienteService.registrarClienteDesdePos(dto);
        return ResponseEntity.ok(nuevoUsuario);
    }

    @GetMapping("/mis-cupones")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<CuponDTO>> getMisCupones(Principal principal) {
        // Principal contiene el email extraído del Token JWT
        String userEmail = principal.getName();
        List<CuponDTO> misCupones = cuponService.obtenerCuponesPorUsuario(userEmail);
        return ResponseEntity.ok(misCupones);
    }
}