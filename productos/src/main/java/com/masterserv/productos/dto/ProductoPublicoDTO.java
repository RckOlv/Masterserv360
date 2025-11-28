package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductoPublicoDTO {
    
    private Long id;
    private String nombre;
    
    // --- ESTE ES EL CAMPO QUE FALTABA ---
    private String codigo; 
    // ------------------------------------
    
    private String descripcion;
    private BigDecimal precioVenta;
    private Integer stockActual;
    private String imagenUrl;
    private String nombreCategoria;
}