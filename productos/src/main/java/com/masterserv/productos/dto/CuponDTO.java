package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoCupon;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CuponDTO {
    private Long id;
    private String codigo;
    private BigDecimal descuento;
    private LocalDate fechaVencimiento;
    private EstadoCupon estado;
    private String clienteEmail; // Para mostrar a quién pertenece
}