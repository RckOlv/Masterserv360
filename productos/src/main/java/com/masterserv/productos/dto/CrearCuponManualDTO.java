package com.masterserv.productos.dto;

import com.masterserv.productos.enums.TipoDescuento;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CrearCuponManualDTO {
    
    private Long usuarioId;         // ID del cliente al que le regalamos el cupón
    private BigDecimal valor;       // Monto ($1000) o Porcentaje (10%)
    private TipoDescuento tipoDescuento; // FIJO o PORCENTAJE
    private int diasValidez;        // Cuántos días durará (ej: 30)
    private String motivo;          // Ej: "Regalo cumpleaños", "Compensación error"
}