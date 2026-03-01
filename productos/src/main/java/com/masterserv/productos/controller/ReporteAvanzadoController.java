package com.masterserv.productos.controller;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.StockInmovilizadoResponse;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.service.PdfService;
import com.masterserv.productos.service.ReporteAvanzadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reportes-avanzados")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteAvanzadoController {

    @Autowired private ReporteAvanzadoService reporteService;
    @Autowired private PdfService pdfService; // ✅ Inyectamos el PdfService

    // ==========================================
    // JSON ENDPOINTS (Los que ya tenías para la vista)
    // ==========================================
    
    @GetMapping("/valorizacion")
    public ResponseEntity<List<ValorizacionInventarioDTO>> getValorizacion() {
        return ResponseEntity.ok(reporteService.getValorizacionInventario());
    }

    @GetMapping("/inmovilizado")
    public ResponseEntity<List<StockInmovilizadoResponse>> getStockInmovilizado(
            @RequestParam(defaultValue = "90") int dias) {
        return ResponseEntity.ok(reporteService.obtenerStockInmovilizado(dias));
    }

    @GetMapping("/historial-costos/buscar")
    public ResponseEntity<List<VariacionCostoDTO>> buscarHistorial(@RequestParam String nombre) {
        return ResponseEntity.ok(reporteService.buscarCostosPorNombre(nombre));
    }

    @GetMapping("/historial-costos-general")
    public ResponseEntity<List<VariacionCostoDTO>> getUltimosCostos() {
        return ResponseEntity.ok(reporteService.getUltimosCostosGenerales());
    }

    @GetMapping("/valorizacion/pdf")
    public ResponseEntity<byte[]> descargarPdfValorizacion() {
        List<ValorizacionInventarioDTO> datos = reporteService.getValorizacionInventario();
        byte[] pdfBytes = pdfService.generarReporteValorizacionPdf(datos);
        return construirRespuestaPdf(pdfBytes, "Valorizacion_Inventario.pdf");
    }

    @GetMapping("/inmovilizado/pdf")
    public ResponseEntity<byte[]> descargarPdfInmovilizado(@RequestParam(defaultValue = "90") int dias) {
        List<StockInmovilizadoResponse> response = reporteService.obtenerStockInmovilizado(dias);
        
        @SuppressWarnings("unchecked")
        List<StockInmovilizadoDTO> datos = (List<StockInmovilizadoDTO>)(List<?>) response;
        
        byte[] pdfBytes = pdfService.generarReporteStockInmovilizadoPdf(datos, dias);
        return construirRespuestaPdf(pdfBytes, "Stock_Inmovilizado.pdf");
    }

    @GetMapping("/historial-costos/pdf")
    public ResponseEntity<byte[]> descargarPdfHistorialCostos(@RequestParam(required = false) String nombre) {
        List<VariacionCostoDTO> datos;
        if (nombre != null && !nombre.isBlank()) {
            datos = reporteService.buscarCostosPorNombre(nombre);
        } else {
            datos = reporteService.getUltimosCostosGenerales();
        }
        byte[] pdfBytes = pdfService.generarReporteEvolucionCostosPdf(datos, nombre != null ? nombre : "Generales");
        return construirRespuestaPdf(pdfBytes, "Evolucion_Costos.pdf");
    }

    private ResponseEntity<byte[]> construirRespuestaPdf(byte[] contenido, String nombreArchivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", nombreArchivo);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(contenido, headers, HttpStatus.OK);
    }
}