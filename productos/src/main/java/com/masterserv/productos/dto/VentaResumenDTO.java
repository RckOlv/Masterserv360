package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VentaResumenDTO {
    private Long id;
    private LocalDateTime fechaVenta;
    private BigDecimal totalVenta;
    private EstadoVenta estado;
    
    // Campos obligatorios para que el Mapper funcione:
    private String clienteNombre;
    private Integer cantidadItems;
    private String codigoCuponUsado;
}