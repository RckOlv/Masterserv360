package com.masterserv.productos.controller;

import com.masterserv.productos.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard") // <-- La ruta API correcta
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/estadisticas")
    public ResponseEntity<Map<String, Object>> getEstadisticasDashboard() {
        Map<String, Object> estadisticas = dashboardService.getEstadisticas();
        return ResponseEntity.ok(estadisticas);
    }
}