package com.masterserv.productos.dto; // Ajusta el paquete

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AbrirCajaDTO {
    private Long usuarioId;
    private BigDecimal montoInicial;
}