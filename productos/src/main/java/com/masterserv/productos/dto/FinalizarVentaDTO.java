package com.masterserv.productos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FinalizarVentaDTO {

    @NotNull(message = "El ID del vendedor es obligatorio")
    private Long vendedorId; // El Usuario (vendedor) que está logueado

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId; // El Usuario (cliente) que el vendedor seleccionó

    // El carritoId se puede obtener a partir del vendedorId en el backend
}