package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoUsuario;
import lombok.Data;
import java.util.List; // Mentor: Usamos List en lugar de Set

@Data
public class UsuarioDTO {
    
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash; // Solo para enviar, nunca para recibir
    private String documento;
    private String telefono;
    private EstadoUsuario estado;
    
    private Long tipoDocumentoId;
    private String tipoDocumentoNombre; // Para mostrar en la tabla (viene de nombreCorto)

    // --- Mentor: CORRECCIÓN CRÍTICA ---
    // Cambiamos de Set a List para ser consistentes con el frontend
    private List<RolDTO> roles; 
    // --- Fin Corrección ---
}