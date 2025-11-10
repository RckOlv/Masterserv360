package com.masterserv.productos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para que un cliente actualice su propio perfil.
 * Nótese que NO se incluye el email, ya que es el
 * identificador de login y no debería ser modificado por esta vía.
 * Tampoco se incluye el cambio de contraseña (eso sería un endpoint aparte).
 */
@Data
public class ClientePerfilUpdateDTO {

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido no puede estar vacío")
    @Size(max = 100)
    private String apellido;

    @Size(max = 20)
    private String telefono;

    // (Asumo que TipoDocumento y Documento son obligatorios)
    
    @NotNull(message = "El ID del tipo de documento es obligatorio")
    private Long tipoDocumentoId; 

    @NotBlank(message = "El número de documento no puede estar vacío")
    @Size(max = 20)
    private String documento;
}