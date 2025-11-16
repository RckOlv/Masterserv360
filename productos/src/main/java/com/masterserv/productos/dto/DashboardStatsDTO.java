package com.masterserv.productos.dto;

import java.math.BigDecimal;
import lombok.Data; // Asumo que usas Lombok por tus otros DTOs

@Data // Genera getters, setters, etc.
public class DashboardStatsDTO {

    private BigDecimal totalVentasMes;
    private long productosBajoStock;
    private long clientesActivos;
    
    // --- Mentor: CAMPO NUEVO AÑADIDO ---
    private BigDecimal totalVentasHoy;
    
    // (Podríamos añadir más, como pedidosPendientes)
}