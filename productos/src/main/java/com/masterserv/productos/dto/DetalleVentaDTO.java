package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DetalleVentaDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}