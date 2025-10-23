package com.masterserv.productos.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequestDTO {

    @NotEmpty(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    private String email;

    @NotEmpty(message = "La contraseña no puede estar vacía")
    private String password;
}