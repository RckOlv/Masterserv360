package com.masterserv.productos.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AuditoriaFiltroDTO {
    // Filtros de texto
    private String usuario; // Buscaremos por coincidencia parcial (LIKE)
    private String accion;  // CREAR, ACTUALIZAR, ELIMINAR (Exacto)
    private String entidad; // Producto, Usuario, etc. (Exacto)
    
    // Rango de fechas
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
}