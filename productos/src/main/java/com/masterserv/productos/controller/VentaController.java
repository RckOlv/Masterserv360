package com.masterserv.productos.controller;

import com.masterserv.productos.dto.VentaDTO;
import com.masterserv.productos.dto.VentaFiltroDTO; // <-- Importar DTO Filtro
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.service.PdfService;
import com.masterserv.productos.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // Mentor: Import necesario
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal; // ¡Importante para obtener el vendedor!

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private PdfService pdfService;

    private static final Logger logger = LoggerFactory.getLogger(VentaController.class);

    /**
     * Endpoint principal para crear (finalizar) una nueva venta.
     */
    @PostMapping // Correcto: POST a /api/ventas
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
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long id) {
        
        // 1. Buscamos la venta completa (usando el nuevo helper del Service)
        Venta ventaCompleta = ventaService.findVentaByIdWithDetails(id); 

        // 2. Generamos el PDF (Tu PdfService ya hace esto)
        byte[] pdfBytes = pdfService.generarComprobanteVenta(ventaCompleta);

        // 3. Preparamos los Headers de la respuesta para forzar la descarga
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Comprobante-Venta-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        // 4. Devolvemos los bytes del PDF
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /**
     * Obtiene una venta por su ID (con detalles).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<VentaDTO> getVentaById(@PathVariable Long id) {
        VentaDTO venta = ventaService.findById(id); // Usa findByIdWithDetails internamente
        return ResponseEntity.ok(venta);
    }

    /**
     * Obtiene todas las ventas de forma paginada (SIN filtros).
     * Mentor: ESTE ENDPOINT YA NO SE USA, PERO LO DEJAMOS.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> getAllVentas(Pageable pageable) {
        Page<VentaDTO> ventas = ventaService.findAll(pageable); // Llama al método sin filtros
        return ResponseEntity.ok(ventas);
    }

    // --- ¡ENDPOINT DE FILTRADO MODIFICADO! ---
    /**
     * Obtiene las ventas filtradas según criterios y paginación.
     * Este endpoint ahora aplica seguridad basada en rol:
     * - ADMIN: Puede filtrar por cualquier vendedor (o todos).
     * - VENDEDOR: Solo verá sus propias ventas, ignorando el filtro 'vendedorId'.
     */
    @PostMapping("/filtrar") // Usamos POST para enviar el cuerpo del filtro
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<Page<VentaDTO>> findVentasByCriteria(
            @RequestBody VentaFiltroDTO filtro, 
            Pageable pageable,
            Authentication authentication) { // Mentor: Inyectamos Authentication
        
        // Verificamos si el usuario es VENDEDOR
        boolean isVendedor = authentication.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_VENDEDOR"));
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && isVendedor) {
            // Si es VENDEDOR y NO es ADMIN, forzamos el filtro a su propio ID
            String vendedorEmail = authentication.getName();
            logger.info("Filtrando ventas solo para el VENDEDOR: {}", vendedorEmail);
            // Llamamos a un método de servicio diferente que fuerza el ID
            Page<VentaDTO> ventasFiltradas = ventaService.findByCriteriaForVendedor(filtro, pageable, vendedorEmail);
            return ResponseEntity.ok(ventasFiltradas);

        } else if (isAdmin) {
            // Si es ADMIN, puede ver todo. Le pasamos el filtro tal cual.
            logger.info("Filtrando ventas como ADMIN.");
            Page<VentaDTO> ventasFiltradas = ventaService.findByCriteria(filtro, pageable);
            return ResponseEntity.ok(ventasFiltradas);
        }

        // Si no es ninguno (raro, pero posible), no devolvemos nada.
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    // ------------------------------------


    /**
     * Cancela una venta (y repone el stock).
     */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('ADMIN')") // Solo Admin puede cancelar
    public ResponseEntity<Void> cancelarVenta(@PathVariable Long id, Principal principal) {
           if (principal == null || principal.getName() == null) {
               return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
           }
           String usuarioEmailCancela = principal.getName();
           ventaService.cancelarVenta(id, usuarioEmailCancela);
           return ResponseEntity.noContent().build(); // 204 No Content
    }
}