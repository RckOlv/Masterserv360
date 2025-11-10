package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductoPublicoFiltroDTO {
    // Filtros para el cliente
    private String nombre;
    private List<Long> categoriaIds;
    private BigDecimal precioMin;
    private BigDecimal precioMax;
    private Boolean soloConStock; // (ej: true si solo quiere ver productos con stock > 0)
}