package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ClienteFidelidadDTO {
    private Long clienteId;
    private String nombreCompleto;
    
    // Puntos
    private Integer puntosAcumulados;
    private String equivalenciaMonetaria; // Ej: "equivale a $5000"
    
    // Cupones listos para usar
    private List<CuponDTO> cuponesDisponibles; 
}