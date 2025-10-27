package com.masterserv.productos.controller;

import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // Para saber qué usuario está logueado
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
// Vendedores y Admin pueden gestionar pedidos
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") 
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    /**
     * Crea un nuevo Pedido (en estado PENDIENTE).
     */
    @PostMapping
    public ResponseEntity<PedidoDTO> createPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        // (En un futuro, el usuarioId debería tomarse del 'Principal' (token)
        // pero por ahora lo dejamos como viene en el DTO)
        PedidoDTO nuevoPedido = pedidoService.create(pedidoDTO);
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    /**
     * Obtiene el detalle de un Pedido.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.findById(id));
    }
    
    // (Aquí faltaría el GET /filtrar paginado, lo añadimos después)

    /**
     * Marca un pedido como COMPLETADO y actualiza el stock.
     */
    @PatchMapping("/{id}/completar")
    public ResponseEntity<Void> completarPedido(@PathVariable Long id, Principal principal) {
        // Obtenemos el usuario logueado (el que confirma)
        // Esto requiere que el 'principal.getName()' sea el email y buscarlo
        // Simplificación por ahora: asumimos que el service lo maneja con un ID fijo
        // Long usuarioId = ... (buscar usuario por email principal.getName())
        
        // TODO: Obtener el ID del usuario desde el 'principal'
        Long usuarioIdQueConfirma = 1L; // ¡HARDCODEADO! Arreglar esto
        
        pedidoService.marcarPedidoCompletado(id, usuarioIdQueConfirma);
        return ResponseEntity.noContent().build(); // 204 Éxito
    }

    /**
     * Marca un pedido como CANCELADO.
     */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarPedido(@PathVariable Long id) {
        pedidoService.marcarPedidoCancelado(id);
        return ResponseEntity.noContent().build();
    }
}