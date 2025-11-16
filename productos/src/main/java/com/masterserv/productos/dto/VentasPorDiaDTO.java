package com.masterserv.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para el gráfico de ventas diarias.
 */
public record VentasPorDiaDTO(
    LocalDate fecha,
    BigDecimal total
) {
}