package com.masterserv.productos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Representa el precio que el proveedor pone a UN item.
 * Recibiremos una lista de estos.
 */
@Data
public class ItemOfertaDTO {
    
    @NotNull
    private Long itemCotizacionId; // El ID del ItemCotizacion que está cotizando

    @NotNull
    @Positive(message = "El precio debe ser mayor a cero")
    private BigDecimal precioUnitarioOfertado;
}