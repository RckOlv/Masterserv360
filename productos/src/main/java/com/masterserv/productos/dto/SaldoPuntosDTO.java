package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class SaldoPuntosDTO {
    private Integer saldoPuntos;      // ej: 500
    private BigDecimal valorMonetario; // ej: 750.00
    private String equivalenciaActual; // ej: "100 Puntos = $150 ARS"
}