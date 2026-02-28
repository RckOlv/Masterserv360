package com.masterserv.productos.controller;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.StockInmovilizadoResponse;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.service.ReporteAvanzadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reportes-avanzados")
@PreAuthorize("hasRole('ADMIN')")
public class ReporteAvanzadoController {

    @Autowired private ReporteAvanzadoService reporteService;

    // GET /api/reportes-avanzados/valorizacion
    @GetMapping("/valorizacion")
    public ResponseEntity<List<ValorizacionInventarioDTO>> getValorizacion() {
        return ResponseEntity.ok(reporteService.getValorizacionInventario());
    }

    // GET /api/reportes-avanzados/inmovilizado?dias=90
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
}