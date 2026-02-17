package com.masterserv.productos.dto.reporte;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockInmovilizadoDTO {
    Long getProductoId();
    String getNombre();
    String getCategoria();
    Integer getStockActual();
    BigDecimal getCostoUnitario();
    BigDecimal getCapitalParado(); // Stock * Costo
    LocalDateTime getUltimaVenta();
    Integer getDiasSinVenta();
}