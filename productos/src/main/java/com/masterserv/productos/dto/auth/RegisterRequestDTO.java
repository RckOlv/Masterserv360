package com.masterserv.productos.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotEmpty
    @Size(min = 2, max = 50)
    private String nombre;

    @NotEmpty
    @Size(min = 2, max = 50)
    private String apellido;

    @NotEmpty
    @Email
    @Size(max = 100)
    private String email;

    @NotEmpty
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    // Campos opcionales (pueden venir null)
    private Long tipoDocumentoId;
    
    @Size(max = 30)
    private String documento;

    @Size(max = 20)
    private String telefono;
}