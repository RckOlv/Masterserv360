package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor // Genera constructor con TODOS los campos en orden
@NoArgsConstructor
public class AuditoriaDTO {
    private Long id;
    private String entidad;
    private String entidadId;
    private String accion;
    private String usuario;
    private LocalDateTime fecha;
    private String detalle;
    
    // Nuevos campos
    private String valorAnterior;
    private String valorNuevo;
    private String motivo;
}