package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetallePedidoDTO {

    private Long id; // Solo lectura

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    // Solo lectura (nombre y código vienen desde la entidad Producto)
    private String productoNombre;
    private String productoCodigo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private int cantidad;

    @NotNull(message = "El precio unitario (costo) es obligatorio")
    private BigDecimal precioUnitario; // Precio congelado al momento del pedido

    private BigDecimal subtotal; // Solo lectura (precioUnitario * cantidad)
}
