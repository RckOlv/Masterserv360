package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoDescuento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para transferir información de Recompensas entre el frontend y el backend.
 * Usado por el Admin en la pantalla de "Reglas de Puntos" y por el Cliente al canjear.
 */
@Data
@NoArgsConstructor
public class RecompensaDTO {

    private Long id;

    @NotEmpty(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "Los puntos requeridos son obligatorios")
    @Min(value = 1, message = "Se requiere al menos 1 punto")
    private int puntosRequeridos;

    @NotNull(message = "El tipo de descuento es obligatorio")
    private TipoDescuento tipoDescuento;

    @NotNull(message = "El valor del descuento es obligatorio")
    @Min(value = 1, message = "El valor debe ser al menos 1")
    private BigDecimal valor; // (Ej: 500 para $500 FIJO, o 20 para 20%)

    // ID de la Regla de Puntos a la que pertenece
    // (Es @NotNull en el backend, pero el frontend no lo necesita al leer)
    private Long reglaPuntosId;

    // Opcional: ID de la categoría a la que aplica
    private Long categoriaId;

    // --- Campo de solo Lectura ---
    
    // Nombre de la categoría (para mostrar en la lista)
    private String categoriaNombre; 
}