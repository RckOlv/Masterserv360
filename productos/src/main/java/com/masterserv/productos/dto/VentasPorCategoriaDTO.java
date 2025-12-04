package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VentasPorCategoriaDTO {
    private String categoria;
    private BigDecimal total;
}