package com.masterserv.productos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CanjePuntosRequestDTO {
    
    // El frontend enviará el email del cliente (o lo tomamos del Principal)
    // private String clienteEmail; 

    @NotNull(message = "Debe especificar la cantidad de puntos a canjear")
    @Positive(message = "La cantidad de puntos debe ser positiva")
    private Integer puntosACanjear;
}