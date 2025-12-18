package com.masterserv.productos.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private Long usuarioId;
    private String email;
    private List<String> roles;
    private List<String> permisos; 
    
    // --- NUEVO CAMPO ---
    private boolean debeCambiarPassword; 
}