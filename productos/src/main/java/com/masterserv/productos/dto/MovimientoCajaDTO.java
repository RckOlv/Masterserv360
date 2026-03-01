package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MovimientoCajaDTO {
    private Long id;
    private LocalDateTime fecha;
    private String tipoMovimiento;
    private String concepto;
    private BigDecimal monto;
    private String metodoPago;
    private String usuarioNombre;
}