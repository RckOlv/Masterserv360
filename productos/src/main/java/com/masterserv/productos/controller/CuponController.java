package com.masterserv.productos.controller;

import com.masterserv.productos.dto.CrearCuponManualDTO;
import com.masterserv.productos.dto.CuponDTO;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.mapper.CuponMapper;
import com.masterserv.productos.repository.UsuarioRepository;
import com.masterserv.productos.service.CuponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    @Autowired
    private CuponService cuponService;

	@Autowired
    private CuponMapper cuponMapper;

	@Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Endpoint para que el ADMIN cree cupones manualmente.
     * URL: POST /api/cupones/manual
     */
    @PostMapping("/manual")
    @PreAuthorize("hasRole('ADMIN')") // <-- Seguridad: Solo admin
    public ResponseEntity<CuponDTO> crearCuponManual(@RequestBody CrearCuponManualDTO request) {
        
        CuponDTO nuevoCupon = cuponService.crearCuponManual(
            request.getUsuarioId(),
            request.getValor(),
            request.getTipoDescuento(),
            request.getDiasValidez(),
            request.getMotivo()
        );
        
        return ResponseEntity.ok(nuevoCupon);
    }

	/**
     * Valida un cupón para mostrar el descuento en el POS antes de la venta.
     * NO lo consume, solo verifica validez.
     */
    @GetMapping("/validar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<CuponDTO> validarCuponParaVenta(
            @RequestParam String codigo,
            @RequestParam Long clienteId) {
        
        // Buscamos al cliente para validar pertenencia
        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Usamos el servicio existente que YA TIENE las validaciones (fecha, estado, dueño)
        // Nota: El servicio devuelve la Entidad, la convertimos a DTO para enviarla
        var cuponEntidad = cuponService.validarCupon(codigo, cliente);
        
        return ResponseEntity.ok(cuponMapper.toCuponDTO(cuponEntidad));
    }
}