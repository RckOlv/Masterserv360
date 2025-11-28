package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoUsuario;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; 

@Data
public class UsuarioDTO {
    
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash; 
    private String documento;
    private String telefono;
    private EstadoUsuario estado;
    
    private Long tipoDocumentoId;
    private String tipoDocumentoNombre; 

    private Long idTipoDocumento; // (Este lo usá
    // bamos antes, lo dejamos por compatibilidad)

    // --- NUEVO CAMPO: Para buscar por nombre ("DNI") ---
    private String tipoDocumentoBusqueda; 
    // ---------------------------------------------------
    
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    private List<RolDTO> roles; 
}