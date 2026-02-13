package com.masterserv.productos.dto;

import java.math.BigDecimal;

public interface ResumenProductoCompraDTO {
    Long getProductoId();
    String getNombre();
    String getCodigo();
    String getImagenUrl();
    Long getCantidadCotizaciones(); // Cuántos proveedores ofertaron
    BigDecimal getMejorPrecio();    // El precio más bajo encontrado
}