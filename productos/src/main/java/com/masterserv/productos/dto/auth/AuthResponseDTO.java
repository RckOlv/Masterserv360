package com.masterserv.productos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Útil para crear la respuesta
public class AuthResponseDTO {
    
    private String token;
    private String tipo = "Bearer";

    public AuthResponseDTO(String token) {
        this.token = token;
    }
}