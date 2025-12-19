package com.masterserv.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class DashboardStatsDTO {

    private BigDecimal totalVentasMes;
    private long productosBajoStock;
    private long clientesActivos;
    private BigDecimal totalVentasHoy;
    
    // --- NUEVO CAMPO PARA EL REPORTE PDF ---
    private long cantidadVentasPeriodo; // Cantidad de tickets emitidos en el rango de fechas
    
    private List<PedidoEnCaminoDTO> pedidosEnCamino;

    @Data
    public static class PedidoEnCaminoDTO {
        private String proveedor;
        private LocalDate fechaEntrega;
        private Long diasRestantes; 
    }
}