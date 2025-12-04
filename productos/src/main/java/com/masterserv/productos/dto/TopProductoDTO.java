package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera getters y setters (getNombre, getCantidadVendida)
@AllArgsConstructor // Constructor con argumentos para la query JPQL
@NoArgsConstructor // Constructor vacío por si acaso
public class TopProductoDTO {
    
    private Long productoId;
    private String nombre;
    private Long cantidadVendida;
}