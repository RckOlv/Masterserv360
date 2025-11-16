package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List; // Importar List

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolDTO {

    private Long id;

    private String nombreRol;

    private String descripcion;

    // --- Mentor: CAMPO AÑADIDO (Este es el que faltaba) ---
    private List<PermisoDTO> permisos; 
    
    // Nota: El backend ya tiene PermisoDTO y PermisoMapper listos.
}