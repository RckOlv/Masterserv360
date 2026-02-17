package com.masterserv.productos.dto.reporte;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface VariacionCostoDTO {
    String getProducto();
    String getProveedor();
    LocalDateTime getFechaCompra();
    BigDecimal getCostoPagado();
    String getNroOrden();
}