package com.masterserv.productos.dto.auth;

// Mentor: Asumo que usas Lombok (si no, añade getters/setters y el constructor)
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List; // Mentor: Importar List

@Data
@NoArgsConstructor
@AllArgsConstructor // Mentor: Este constructor acepta todos los campos
public class AuthResponseDTO {

    private String token;
    
    // Mentor: Campos añadidos para el frontend
    private Long usuarioId;
    private String email;
    private List<String> roles;
    private List<String> permisos; 
}