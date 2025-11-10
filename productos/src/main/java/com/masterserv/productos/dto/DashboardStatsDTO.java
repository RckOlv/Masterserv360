package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardStatsDTO {
    // Nombres que tu frontend ya espera
    private long productosBajoStock;
    private BigDecimal totalVentasMes;
    private long clientesActivos;
}