package com.masterserv.productos.dto;

import lombok.Data;

@Data
public class AddListaEsperaDTO {
    private Long usuarioId;  // El cliente que quiere esperar
    private Long productoId; // El producto que no tiene stock
}