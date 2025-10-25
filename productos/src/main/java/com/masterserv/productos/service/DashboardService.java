package com.masterserv.productos.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DashboardService {

    /**
     * Este servicio simulará la obtención de estadísticas.
     * Está protegido por @PreAuthorize.
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')") // Solo el ADMIN puede obtener estadísticas
    public Map<String, Object> getEstadisticas() {
        // En un proyecto real, esto haría llamadas a VentaRepository, ProductoRepository, etc.
        return Map.of(
            "totalVentasMes", 1500000.00,
            "productosBajoStock", 12,
            "clientesActivos", 85,
            "mensaje", "Datos del Dashboard cargados exitosamente (ROLE_ADMIN verificado)"
        );
    }
}