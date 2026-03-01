package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoMovimiento;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockDTO {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    private Long usuarioId; 

    private TipoMovimiento tipoMovimiento;

    @NotNull
    private int cantidad; 

    private String usuarioNombre;

    @NotEmpty(message = "El motivo es obligatorio")
    private String motivo;

    private LocalDateTime fecha;

    private Long ventaId;    
    private Long pedidoId;   
}