package com.masterserv.productos.controller;

import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.dto.PedidoDetalladoDTO;
import com.masterserv.productos.entity.Pedido; // <--- Importado
import com.masterserv.productos.repository.PedidoRepository; // <--- Importado
import com.masterserv.productos.service.PdfService; // <--- Importado
import com.masterserv.productos.service.PedidoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/pedidos")
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") 
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;
    
    // --- Inyecciones para PDF ---
    @Autowired
    private PdfService pdfService; 
    
    @Autowired
    private PedidoRepository pedidoRepository;
    // ----------------------------

    /**
     * Crea un nuevo Pedido Manual.
     */
    @PostMapping
    public ResponseEntity<PedidoDTO> createPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        PedidoDTO nuevoPedido = pedidoService.create(pedidoDTO);
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    /**
     * Lista paginada de pedidos.
     */
    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> getAllPedidos(Pageable pageable) {
        Page<PedidoDTO> pedidos = pedidoService.findAll(pageable);
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Obtiene el DTO básico de un Pedido.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.findById(id));
    }
    
    /**
     * Endpoint para el COMPROBANTE VISUAL (JSON).
     */
    @GetMapping("/{id}/detalles")
    public ResponseEntity<PedidoDetalladoDTO> getPedidoDetalles(@PathVariable Long id) {
        PedidoDetalladoDTO dto = pedidoService.obtenerDetallesPedido(id);
        return ResponseEntity.ok(dto);
    }
    
    /**
     * NUEVO ENDPOINT: Descargar Comprobante PDF (Oficial).
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarComprobantePdf(@PathVariable Long id) {
        // Buscamos la ENTIDAD completa para pasársela a PdfService
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));

        byte[] pdfBytes = pdfService.generarComprobantePedido(pedido);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // "inline" para que se abra en el navegador, "attachment" para forzar descarga
        headers.setContentDispositionFormData("inline", "pedido_" + id + ".pdf"); 

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    /**
     * Marca un pedido como COMPLETADO y actualiza el stock.
     */
    @PatchMapping("/{id}/completar")
    public ResponseEntity<Void> completarPedido(@PathVariable Long id, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        
        String userEmail = principal.getName();
        pedidoService.marcarPedidoCompletado(id, userEmail);
        
        return ResponseEntity.noContent().build();
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