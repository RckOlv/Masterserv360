package com.masterserv.productos.dto;

import lombok.Data;

@Data
public class ClienteDTO {
    
    private Long id; // Útil para devolver el ID generado al frontend
    
    private String nombre;
    private String apellido;
    private String email;
    
    private String documento; // El número (ej: 40123456)
    
    private String telefono;
    
    // Este campo recibe "DNI", "CUIT", "PAS" desde el select del modal
    private String tipoDocumentoBusqueda; 
    
    // Lo dejamos por si en el futuro envías el ID numérico del tipo de doc
    private Long tipoDocumentoId; 
}