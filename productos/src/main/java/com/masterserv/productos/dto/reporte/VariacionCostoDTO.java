package com.masterserv.productos.dto.reporte;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface VariacionCostoDTO {
    String getProducto();
    String getProveedor();
    LocalDate getFechaCompra();
    BigDecimal getCostoPagado();
    String getNroOrden();
}