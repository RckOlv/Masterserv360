package com.masterserv.productos.dto.reporte;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockInmovilizadoResponse {
    private Long productoId;
    private String nombre;
    private String categoria;
    private Integer stockActual;
    private BigDecimal costoUnitario;
    private BigDecimal capitalParado;
    private LocalDateTime ultimaVenta;
    private Integer diasSinVenta; // ✅ Java calculará esto

    // Constructor que convierte el DTO de la base de datos en esta Respuesta
    public StockInmovilizadoResponse(StockInmovilizadoDTO dto, Integer diasSinVenta) {
        this.productoId = dto.getProductoId();
        this.nombre = dto.getNombre();
        this.categoria = dto.getCategoria();
        this.stockActual = dto.getStockActual();
        this.costoUnitario = dto.getCostoUnitario();
        this.capitalParado = dto.getCapitalParado();
        this.ultimaVenta = dto.getUltimaVenta();
        this.diasSinVenta = diasSinVenta;
    }
}