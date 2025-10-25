package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoUsuario;
import lombok.Data;

@Data
public class UsuarioFiltroDTO {
    // DTO para recibir los filtros de la UI de admin
    private String nombreOEmail; // Un solo campo para buscar por nombre, apellido o email
    private String documento;
    private Long rolId;
    private EstadoUsuario estado; // Usamos el Enum
}