package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemCarritoDTO {
    private Long id; // ID del ItemCarrito (para poder borrarlo)
    private Long productoId;
    private String productoNombre;
    private BigDecimal precioUnitario;
    private int cantidad;
    private BigDecimal subtotal;
}