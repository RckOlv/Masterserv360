package com.masterserv.productos.dto;

import com.masterserv.productos.enums.EstadoPedido;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PedidoFiltroDTO {
    // Filtros opcionales (pueden venir nulos)
    private Long proveedorId;
    private Long usuarioId; // Quién lo pidió
    private EstadoPedido estado;
    
    // Rango de fechas (Fecha Pedido)
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
}