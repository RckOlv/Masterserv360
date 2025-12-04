package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoMovimiento;
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

    // MENTOR: Quitamos @NotNull porque esto lo obtenemos del Token en el Controller
    private Long usuarioId; 

    // MENTOR: Quitamos @NotNull porque esto lo definimos en el Service como AJUSTE_MANUAL
    private TipoMovimiento tipoMovimiento;

    // MENTOR: Quitamos @Min(1) para permitir números negativos (restar stock)
    @NotNull
    private int cantidad; 

    @NotEmpty(message = "El motivo es obligatorio")
    private String motivo;

    // --- Campos Opcionales para Trazabilidad ---
    private Long ventaId;    
    private Long pedidoId;   
}