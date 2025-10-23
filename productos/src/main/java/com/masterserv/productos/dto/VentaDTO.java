package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class VentaDTO {
    private Long id;
    private LocalDateTime fechaVenta;
    private EstadoVenta estado;
    private BigDecimal totalVenta;
    private Long vendedorId;
    private String vendedorNombre;
    private Long clienteId;
    private String clienteNombre;
    private List<DetalleVentaDTO> detalles;
}