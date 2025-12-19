package com.masterserv.productos.controller;

import com.masterserv.productos.dto.ConfirmacionPedidoDTO;
import com.masterserv.productos.dto.CotizacionPublicaDTO;
import com.masterserv.productos.dto.OfertaProveedorDTO;
import com.masterserv.productos.repository.PedidoRepository;
import com.masterserv.productos.service.CotizacionService;
import com.masterserv.productos.service.PedidoService; // <--- Importante

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador para endpoints PÚBLICOS que no requieren autenticación JWT.
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private CotizacionService cotizacionService;

    @Autowired
    private PedidoRepository pedidoRepository; 

    // --- ¡AQUÍ ESTABA EL ERROR! FALTABA INYECTAR ESTE SERVICIO ---
    @Autowired
    private PedidoService pedidoService; 
    // -------------------------------------------------------------

    /**
     * Endpoint GET para que el proveedor VEA la solicitud de cotización.
     */
    @GetMapping("/oferta/{token}")
    public ResponseEntity<?> getOfertaPorToken(@PathVariable String token) {
        try {
            CotizacionPublicaDTO cotizacionDTO = cotizacionService.findCotizacionPublicaByToken(token);
            return ResponseEntity.ok(cotizacionDTO);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Error interno."));
        }
    }

    /**
     * Endpoint POST para que el proveedor ENVÍE su oferta.
     */
    @PostMapping("/oferta/{token}")
    public ResponseEntity<?> submitOfertaProveedor(@PathVariable String token, @Valid @RequestBody OfertaProveedorDTO ofertaDTO) {
        try {
            cotizacionService.submitOfertaProveedor(token, ofertaDTO);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Oferta recibida exitosamente."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Error interno."));
        }
    }

    // ================================================================
    //  PEDIDOS: GESTIÓN PÚBLICA POR TOKEN
    // ================================================================
    
    /**
     * Obtiene los datos del pedido para mostrar en la pantalla del proveedor.
     */
    @GetMapping("/pedido/{token}")
    public ResponseEntity<?> getPedidoPorToken(@PathVariable String token) {
        return pedidoRepository.findByToken(token)
            .map(pedido -> {
                Map<String, Object> response = new HashMap<>();
                
                response.put("id", pedido.getId());
                response.put("fecha", pedido.getFechaPedido()); 
                response.put("estado", pedido.getEstado());
                
                if (pedido.getProveedor() != null) {
                    response.put("proveedor", pedido.getProveedor().getRazonSocial());
                }

                List<Map<String, Object>> detalles = pedido.getDetalles().stream().map(d -> {
                    Map<String, Object> item = new HashMap<>();
                    // AGREGAMOS EL ID DEL PRODUCTO (NECESARIO PARA LA CONFIRMACIÓN)
                    item.put("productoId", d.getProducto().getId()); 
                    item.put("producto", d.getProducto().getNombre());
                    item.put("cantidad", d.getCantidad());
                    // Opcional: Enviamos precio actual por si quieren verlo
                    item.put("precio", d.getPrecioUnitario()); 
                    return item;
                }).collect(Collectors.toList());
                
                response.put("detalles", detalles);

                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recibe la confirmación (fecha y precios) del proveedor.
     */
    @PostMapping("/pedido/{token}/confirmar")
    public ResponseEntity<?> confirmarPedidoProveedor(@PathVariable String token, @RequestBody ConfirmacionPedidoDTO dto) {
        try {
            pedidoService.confirmarPedidoPorProveedor(token, dto);
            return ResponseEntity.ok(Map.of("status", "ok", "message", "Pedido confirmado. ¡Gracias!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}