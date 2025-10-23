package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId; // El usuario que REALIZA el movimiento (Vendedor/Admin)

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipoMovimiento;

    @NotNull
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private int cantidad; // Siempre un número positivo. El 'tipo' define si suma o resta.

    @NotEmpty(message = "El motivo es obligatorio")
    private String motivo;

    // --- Campos Opcionales para Trazabilidad ---
    private Long ventaId;    // ID de la Venta que disparó la salida
    private Long pedidoId;   // ID del Pedido que disparó la entrada
}