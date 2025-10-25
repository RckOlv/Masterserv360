package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO para recibir los criterios de filtrado de productos.
 * Todos los campos son opcionales (anulables).
 */
@Data
public class ProductoFiltroDTO {
    private String nombre;
    private String codigo;
    private Long categoriaId;
    private BigDecimal precioMax;
    private Boolean conStock; // true = con stock, false = sin stock
    private String estado;
}