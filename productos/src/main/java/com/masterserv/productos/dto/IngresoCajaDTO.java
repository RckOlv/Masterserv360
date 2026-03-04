package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class IngresoCajaDTO {
    private Long cajaId;
    private BigDecimal monto;
    private String motivo;
}