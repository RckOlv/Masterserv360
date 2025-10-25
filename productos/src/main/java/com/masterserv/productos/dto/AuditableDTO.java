package com.masterserv.productos.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Clase base para DTOs que refleja la auditoría de la Entidad.
 * Usada para que MapStruct pueda mapear fechaCreacion/Modificacion.
 */
@Data
public abstract class AuditableDTO {
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}