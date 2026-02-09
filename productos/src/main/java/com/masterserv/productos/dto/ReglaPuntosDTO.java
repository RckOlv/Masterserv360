package com.masterserv.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate; // <--- AGREGAR ESTE IMPORT
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReglaPuntosDTO extends AuditableDTO {

    private Long id;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotNull(message = "El monto de gasto no puede ser nulo")
    @Positive(message = "El monto de gasto debe ser positivo")
    private BigDecimal montoGasto;

    @NotNull(message = "Los puntos ganados no pueden ser nulos")
    @Positive(message = "Los puntos ganados deben ser positivos")
    private Integer puntosGanados;

    @Positive(message = "La equivalencia de puntos debe ser positiva")
    private BigDecimal equivalenciaPuntos;

    private String estadoRegla;

    private Integer caducidadPuntosMeses;

    private LocalDateTime fechaInicioVigencia;
    
    // --- NUEVO CAMPO CALCULADO ---
    private LocalDate fechaVencimiento; 
}