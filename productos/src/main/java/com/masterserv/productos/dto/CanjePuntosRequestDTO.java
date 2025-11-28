package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de canje de puntos desde el frontend V2.
 * Ahora envía el ID de la recompensa que el cliente seleccionó.
 */
@Data
@NoArgsConstructor
public class CanjePuntosRequestDTO {

    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // (Cambiamos el campo de 'puntosACanjear' a 'recompensaId')

    @NotNull(message = "Debe seleccionar una recompensa.")
    @Min(value = 1, message = "El ID de la recompensa no es válido.")
    private Long recompensaId; // <-- CAMPO ACTUALIZADO

    // --- Mentor: FIN DE LA MODIFICACIÓN ---
    
    // (El getter ahora será getRecompensaId())
}