package com.masterserv.productos.controller;

import com.masterserv.productos.dto.DashboardFilterDTO;
import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.VentasPorCategoriaDTO;
import com.masterserv.productos.dto.VentasPorDiaDTO;
import com.masterserv.productos.dto.UsuarioDTO;
import com.masterserv.productos.service.DashboardService;
import com.masterserv.productos.service.PdfService;
import com.masterserv.productos.service.UsuarioService; // Importar servicio de usuarios

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private DashboardService dashboardService;
    @Autowired private PdfService pdfService;
    
    // --- MENTOR: Inyectamos UsuarioService para obtener el nombre real ---
    @Autowired private UsuarioService usuarioService; 

    @PostMapping("/estadisticas-filtradas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DashboardStatsDTO> getEstadisticasFiltradas(@RequestBody DashboardFilterDTO filtro) {
        return ResponseEntity.ok(dashboardService.getEstadisticasFiltradas(filtro.getFechaInicio(), filtro.getFechaFin()));
    }

    @GetMapping("/ventas-semanales")
    public ResponseEntity<List<VentasPorDiaDTO>> getVentasSemanales() {
        return ResponseEntity.ok(dashboardService.getVentasUltimos7Dias());
    }
    
    @PostMapping("/ventas-semanales")
    public ResponseEntity<List<VentasPorDiaDTO>> getVentasSemanalesFiltradas(@RequestBody DashboardFilterDTO filtro) {
        return ResponseEntity.ok(dashboardService.getVentasPorRango(filtro.getFechaInicio(), filtro.getFechaFin()));
    }

    @PostMapping("/top-productos")
    public ResponseEntity<List<TopProductoDTO>> getTopProductosFiltrados(@RequestBody DashboardFilterDTO filtro) {
        return ResponseEntity.ok(dashboardService.getTopProductosPorRango(filtro.getFechaInicio(), filtro.getFechaFin()));
    }
    
    @GetMapping("/top-productos")
    public ResponseEntity<List<TopProductoDTO>> getTopProductos() {
        return ResponseEntity.ok(dashboardService.getTop5ProductosDelMes());
    }

    @PostMapping("/ventas-categorias")
    public ResponseEntity<List<VentasPorCategoriaDTO>> getVentasPorCategoria(@RequestBody DashboardFilterDTO filtro) {
        return ResponseEntity.ok(dashboardService.getVentasPorCategoria(filtro.getFechaInicio(), filtro.getFechaFin()));
    }

    // --- MENTOR: MÉTODO ACTUALIZADO ---
    @PostMapping("/reporte-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<byte[]> generarReportePdf(@RequestBody DashboardFilterDTO filtro, Principal principal) {
        
        // 1. Obtener nombre del usuario que genera el reporte
        String nombreResponsable = principal.getName(); // Por defecto el email
        try {
             UsuarioDTO u = usuarioService.buscarPorEmail(principal.getName());
             nombreResponsable = u.getNombre() + " " + u.getApellido();
        } catch (Exception e) {
             // Si falla, usamos el email
        }
        
        // 2. Pasarlo al DTO
        filtro.setGeneradoPor(nombreResponsable);

        byte[] pdf = pdfService.generarReporteDashboard(filtro); 

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "reporte_dashboard.pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
    
    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<DashboardStatsDTO> getEstadisticas() {
        return ResponseEntity.ok(dashboardService.getEstadisticas());
    }
}