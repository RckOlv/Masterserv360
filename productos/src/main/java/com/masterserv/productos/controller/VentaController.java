package com.masterserv.productos.controller;

import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.service.PdfService;
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort; // <--- IMPORTADO
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;

@RestController
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private PdfService pdfService;

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> crearVenta(@Valid @RequestBody VentaDTO ventaDTO, Principal principal) {
        if (principal == null || principal.getName() == null) {
             return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String vendedorEmail = principal.getName();
        VentaDTO ventaCreada = ventaService.create(ventaDTO, vendedorEmail);
        return new ResponseEntity<>(ventaCreada, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comprobante")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR', 'CLIENTE')")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long id, Authentication authentication) {
        Venta ventaCompleta = ventaService.findVentaByIdWithDetails(id); 

        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"))) {
            String emailUsuario = authentication.getName();
            if (!ventaCompleta.getCliente().getEmail().equals(emailUsuario)) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }

        byte[] pdfBytes = pdfService.generarComprobanteVenta(ventaCompleta);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Comprobante-Venta-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR', 'CLIENTE')")
    public ResponseEntity<?> getVentaById(@PathVariable Long id, Authentication authentication) {
        try {
            Venta ventaEntity = ventaService.findVentaByIdWithDetails(id);

            boolean esCliente = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

            if (esCliente) {
                String emailUsuario = authentication.getName();
                if (!ventaEntity.getCliente().getEmail().equals(emailUsuario)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("No tienes permiso para ver esta venta.");
                }
            }

            VentaDTO ventaDTO = ventaService.findById(id);
            return ResponseEntity.ok(ventaDTO);

        } catch (Exception e) {
            logger.error("Error al obtener venta: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al cargar la venta.");
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> getAllVentas(
            // ✅ ORDENADO POR FECHA DESCENDENTE
            @PageableDefault(page = 0, size = 10, sort = "fechaVenta", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        
        Page<VentaDTO> ventas = ventaService.findAll(pageable);
        return ResponseEntity.ok(ventas);
    }

    @PostMapping("/filtrar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> findVentasByCriteria(
            @RequestBody VentaFiltroDTO filtro, 
            // ✅ ORDENADO POR FECHA DESCENDENTE TAMBIÉN EN FILTRO
            @PageableDefault(page = 0, size = 10, sort = "fechaVenta", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {
        
        boolean isVendedor = authentication.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_VENDEDOR"));
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && isVendedor) {
            String vendedorEmail = authentication.getName();
            Page<VentaDTO> ventasFiltradas = ventaService.findByCriteriaForVendedor(filtro, pageable, vendedorEmail);
            return ResponseEntity.ok(ventasFiltradas);

        } else if (isAdmin) {
            Page<VentaDTO> ventasFiltradas = ventaService.findByCriteria(filtro, pageable);
            return ResponseEntity.ok(ventasFiltradas);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelarVenta(@PathVariable Long id, Principal principal) {
            if (principal == null || principal.getName() == null) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            String usuarioEmailCancela = principal.getName();
            ventaService.cancelarVenta(id, usuarioEmailCancela);
            return ResponseEntity.noContent().build();
    }
}