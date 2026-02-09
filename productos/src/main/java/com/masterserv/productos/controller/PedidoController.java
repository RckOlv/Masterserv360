package com.masterserv.productos.controller;

import com.masterserv.productos.dto.PedidoDTO;
import com.masterserv.productos.dto.PedidoDetalladoDTO;
import com.masterserv.productos.dto.PedidoFiltroDTO;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.repository.PedidoRepository;
import com.masterserv.productos.service.PdfService;
import com.masterserv.productos.service.PedidoService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <--- IMPORTADO
import org.springframework.data.web.PageableDefault; // <--- IMPORTADO
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
    
    @Autowired
    private PdfService pdfService; 
    
    @Autowired
    private PedidoRepository pedidoRepository;

    @PostMapping
    public ResponseEntity<PedidoDTO> createPedido(@Valid @RequestBody PedidoDTO pedidoDTO) {
        PedidoDTO nuevoPedido = pedidoService.create(pedidoDTO);
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<PedidoDTO>> getAllPedidos(
            // ✅ ORDENADO POR FECHA DESCENDENTE
            @PageableDefault(page = 0, size = 10, sort = "fechaPedido", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        Page<PedidoDTO> pedidos = pedidoService.findAll(pageable);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> getPedidoById(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.findById(id));
    }
    
    @GetMapping("/{id}/detalles")
    public ResponseEntity<PedidoDetalladoDTO> getPedidoDetalles(@PathVariable Long id) {
        PedidoDetalladoDTO dto = pedidoService.obtenerDetallesPedido(id);
        return ResponseEntity.ok(dto);
    }
    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarComprobantePdf(@PathVariable Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));

        byte[] pdfBytes = pdfService.generarComprobantePedido(pedido);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "pedido_" + id + ".pdf"); 

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
    
    @PatchMapping("/{id}/completar")
    public ResponseEntity<Void> completarPedido(@PathVariable Long id, Principal principal) {
        if (principal == null || principal.getName() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userEmail = principal.getName();
        pedidoService.marcarPedidoCompletado(id, userEmail);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Void> cancelarPedido(@PathVariable Long id) {
        pedidoService.marcarPedidoCancelado(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/filtrar")
    public ResponseEntity<Page<PedidoDTO>> filtrarPedidos(
        @RequestBody PedidoFiltroDTO filtro, 
        // ✅ ORDENADO POR FECHA DESCENDENTE TAMBIÉN EN FILTRO
        @PageableDefault(page = 0, size = 10, sort = "fechaPedido", direction = Sort.Direction.DESC)
        Pageable pageable) {
    
    Page<PedidoDTO> resultado = pedidoService.filter(filtro, pageable);
    return ResponseEntity.ok(resultado);
}
}