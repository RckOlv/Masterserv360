package com.masterserv.productos.controller;

import com.masterserv.productos.dto.DashboardStatsDTO;
import com.masterserv.productos.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService; // <-- ¡Inyectar servicio!

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')") // ¡Asegurar!
    public ResponseEntity<DashboardStatsDTO> getEstadisticas() {
        DashboardStatsDTO stats = dashboardService.getEstadisticas();
        return ResponseEntity.ok(stats); // ¡Devolver los datos reales!
    }
}