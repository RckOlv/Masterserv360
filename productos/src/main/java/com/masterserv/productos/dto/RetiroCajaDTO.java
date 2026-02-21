package com.masterserv.productos.dto; // Ajusta tu paquete

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RetiroCajaDTO {
    private Long cajaId;
    private BigDecimal monto;
    private String motivo; // Ej: "Yerba y galletitas"
}