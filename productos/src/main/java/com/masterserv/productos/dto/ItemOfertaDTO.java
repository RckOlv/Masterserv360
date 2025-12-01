package com.masterserv.productos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemOfertaDTO {
    
    @NotNull
    private Long itemCotizacionId;

    // Puede ser null si el proveedor marca "No disponible"
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal precioUnitarioOfertado;

    // --- NUEVOS CAMPOS ---
    
    @Positive(message = "La cantidad debe ser positiva")
    private Integer cantidadOfertada; // Si es null, asumimos la original

    private boolean disponible = true; // true = cotizo, false = no tengo
}