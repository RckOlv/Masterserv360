package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data // @Data está bien aquí, no hay relaciones bidireccionales en DTOs
public class DetalleVentaDTO {

    // --- Campos de Lectura (Para respuestas GET) ---
    private Long id;
    private String productoNombre;
    private String productoCodigo;
    // El precio unitario al que SE VENDIÓ (congelado)
    private BigDecimal precioUnitario;
    // El subtotal calculado (precioUnitario * cantidad)
    private BigDecimal subtotal;

    // --- Campos de Entrada (Para creación POST) ---
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad; // Usamos Integer por consistencia, aunque int también funciona

}