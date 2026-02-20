package com.masterserv.productos.dto; // Ajusta el paquete

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CerrarCajaDTO {
    private Long cajaId;
    private BigDecimal montoDeclarado; // La plata física que el cajero contó en el cajón
}