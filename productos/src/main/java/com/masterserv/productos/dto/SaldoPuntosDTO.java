package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List; // <-- Importar

@Data
public class SaldoPuntosDTO {
    private int saldoPuntos;
    private BigDecimal valorMonetario;
    private String equivalenciaActual;
    private List<RecompensaDTO> recompensasDisponibles;
}