package com.masterserv.productos.controller;

import com.masterserv.productos.dto.FinalizarVentaDTO;
import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    /**
     * Endpoint principal para crear una nueva venta.
     * Recibe el ID del vendedor y del cliente, y procesa el carrito del vendedor.
     */
    @PostMapping("/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> finalizarVenta(@Valid @RequestBody FinalizarVentaDTO finalizarDTO) {
        VentaDTO ventaCreada = ventaService.finalizarVenta(finalizarDTO);
        return new ResponseEntity<>(ventaCreada, HttpStatus.CREATED);
    }

    // Aquí irían otros endpoints (getVentaById, getVentasPorCliente, etc.)
    // que puedes crear usando la misma estructura del ProductoController.
}