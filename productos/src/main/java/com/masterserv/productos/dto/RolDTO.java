package com.masterserv.productos.dto;

import lombok.Data;

@Data
public class RolDTO extends AuditableDTO {
    private Long id;
    private String nombreRol;
    private String descripcion;
    
    // Puedes añadir Set<PermisoDTO> si lo necesitas, pero por ahora lo dejamos simple.
}