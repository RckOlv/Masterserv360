package com.masterserv.productos.dto;

import lombok.Data;
@Data
public class ClientePerfilDTO {
    // Solo datos seguros
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String telefono;
    
    // --- ¡CORRECCIÓN AQUÍ! ---
    // Necesitamos AMBOS para el frontend:
    // El ID para el <select> del formulario
    private Long tipoDocumentoId;
    // El Nombre para mostrarlo en texto plano
    private String tipoDocumento; 
    // -------------------------

    private String documento;
}