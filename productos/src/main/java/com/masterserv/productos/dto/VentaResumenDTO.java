package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoVenta;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para mostrar un resumen de las ventas al cliente
 * en su historial de "Mis Compras".
 */
@Data
public class VentaResumenDTO {

    private Long id; // El ID de la Venta
    private LocalDateTime fechaVenta;
    private EstadoVenta estado;
    private BigDecimal totalVenta;
    private Integer cantidadItems; // Calculado
    
    // (Opcional) El código del cupón que usó, si usó uno
    private String codigoCuponUsado; 
}