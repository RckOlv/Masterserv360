package com.masterserv.productos.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DashboardFilterDTO {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String graficoBase64;
    private String generadoPor; 
}