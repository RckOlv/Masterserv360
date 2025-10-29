package com.masterserv.productos.controller;

import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// --- Imports Añadidos ---
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// --- Fin Imports Añadidos ---

import java.security.Principal; // Para saber qué usuario está logueado
// import java.util.Map; // Este import no se usaba, se puede quitar

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

    // --- ¡NUEVO ENDPOINT AÑADIDO! ---
    /**
     * GET /api/pedidos?page=0&size=10&sort=fechaPedido,desc
     * Obtiene una lista paginada de todos los pedidos.
     * Spring inyecta 'pageable' automáticamente desde los parámetros de la URL.
     * Este método soluciona el error 405 (Method Not Supported).
     */
    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> getAllPedidos(Pageable pageable) {
        Page<PedidoDTO> pedidos = pedidoService.findAll(pageable);
        return ResponseEntity.ok(pedidos);
    }
    // --- Fin del nuevo endpoint ---

    /**
     * Obtiene el detalle de un Pedido.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.findById(id));
    }
    
    /**
     * Marca un pedido como COMPLETADO y actualiza el stock.
     * --- MÉTODO MODIFICADO ---
     * Ahora usa 'Principal' para identificar al usuario que confirma.
     */
    @PatchMapping("/{id}/completar")
    public ResponseEntity<Void> completarPedido(@PathVariable Long id, Principal principal) {
        // 1. Validamos que el Principal (usuario logueado) exista
        if (principal == null || principal.getName() == null) {
             // Esto no debería pasar si Spring Security está bien configurado
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        // 2. Obtenemos el 'username' (que es el email) del Principal
        String userEmail = principal.getName();
        
        // 3. Pasamos el email al servicio. 
        //    ¡No más IDs hardcodeados!
        pedidoService.marcarPedidoCompletado(id, userEmail);
        
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