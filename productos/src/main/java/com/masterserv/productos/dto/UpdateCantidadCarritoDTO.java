package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple utilizado para recibir la nueva cantidad
 * al actualizar un item en el carrito a través del endpoint PUT /api/carrito/items/{id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCantidadCarritoDTO {

    @NotNull(message = "La nueva cantidad es obligatoria")
    // Permitimos 0 porque en la lógica del servicio, una cantidad <= 0 significa quitar el item.
    @Min(value = 0, message = "La cantidad no puede ser negativa")
    private Integer nuevaCantidad;
}