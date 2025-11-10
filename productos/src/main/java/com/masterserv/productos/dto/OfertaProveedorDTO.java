package com.masterserv.productos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa el formulario completo que el proveedor envía.
 */
@Data
public class OfertaProveedorDTO {
    
    @NotNull
    @FutureOrPresent(message = "La fecha de entrega no puede ser en el pasado")
    private LocalDate fechaEntregaOfertada;

    @Valid // ¡Valida cada item de la lista!
    @NotEmpty(message = "Debe cotizar al menos un item")
    private List<ItemOfertaDTO> items;
}