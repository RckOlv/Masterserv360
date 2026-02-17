package com.masterserv.productos.controller;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.service.ReporteAvanzadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes-avanzados")
public class ReporteAvanzadoController {

    @Autowired private ReporteAvanzadoService reporteService;

    // GET /api/reportes-avanzados/valorizacion
    @GetMapping("/valorizacion")
    public ResponseEntity<List<ValorizacionInventarioDTO>> getValorizacion() {
        return ResponseEntity.ok(reporteService.getValorizacionInventario());
    }

    // GET /api/reportes-avanzados/inmovilizado?dias=90
    @GetMapping("/inmovilizado")
    public ResponseEntity<List<StockInmovilizadoDTO>> getStockInmovilizado(
            @RequestParam(defaultValue = "90") int dias) { // Default 90 días
        return ResponseEntity.ok(reporteService.getStockInmovilizado(dias));
    }

    // GET /api/reportes-avanzados/historial-costos/15
    @GetMapping("/historial-costos/{productoId}")
    public ResponseEntity<List<VariacionCostoDTO>> getHistorialCostos(@PathVariable Long productoId) {
        return ResponseEntity.ok(reporteService.getHistorialCostos(productoId));
    }

	@GetMapping("/historial-costos-general")
	public ResponseEntity<List<VariacionCostoDTO>> getUltimosCostos() {
    	return ResponseEntity.ok(reporteService.getUltimosCostosGenerales());
	}
}