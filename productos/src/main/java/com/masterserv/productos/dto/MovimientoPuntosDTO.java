package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoMovimientoPuntos;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MovimientoPuntosDTO {
    private Long id;
    private Integer puntos;
    private TipoMovimientoPuntos tipoMovimiento;
    private LocalDateTime fechaCaducidadPuntos;
    private String descripcion;
    private Long cuentaPuntosId;
    private Long ventaId;
}
