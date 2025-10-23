package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddItemCarritoDTO {
    
    @NotNull(message = "El ID del vendedor es obligatorio")
    private Long vendedorId; // El ID del usuario (vendedor) logueado

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private int cantidad;
}