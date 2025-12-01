package com.masterserv.productos.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitudProductoDTO {
    private Long id;
    private String descripcion;
    private LocalDateTime fechaSolicitud;
    private boolean procesado;
    
    // Datos del usuario "aplanados" para facilitar la lectura en el front
    private String clienteNombre;
    private String clienteTelefono;
    private String clienteEmail;
}