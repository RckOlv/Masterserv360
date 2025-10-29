package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DetallePedidoDTO {

    private Long id; // Solo lectura (útil para respuestas GET)

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId; // Entrada (POST)

    // Solo lectura (útiles para respuestas GET)
    private String productoNombre;
    private String productoCodigo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private int cantidad; // Entrada (POST)

    // --- ¡CORREGIDO! ---
    // Quitamos @NotNull. Este campo ya NO es obligatorio en el POST.
    // El backend lo calculará.
    // Se usará para mostrar el valor en las respuestas GET.
    private BigDecimal precioUnitario; // Solo lectura (calculado por el backend)

    private BigDecimal subtotal; // Solo lectura (calculado por el backend)
}