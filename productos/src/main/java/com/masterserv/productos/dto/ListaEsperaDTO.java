package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListaEsperaDTO {
    private Long id;
    private String fechaSolicitud;
    private String estado;
    
    // Datos del Usuario
    private String usuarioNombre;
    private String usuarioApellido;
    private String usuarioTelefono;
    private String usuarioEmail;

    // Datos del Producto
    private String productoNombre;
    private String productoCodigo;
}