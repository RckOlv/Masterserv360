package com.masterserv.productos.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para la transferencia de datos de Permisos al Frontend (para llenar los checkboxes).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermisoDTO {
    
    private Long id;
    private String nombrePermiso;
    private String descripcion;
    
    // No necesitamos los campos Auditable (fecha_creacion, etc.) aquí.
}