package com.masterserv.productos.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ConfirmacionPedidoDTO {
    private LocalDate fechaEntrega; // La fecha que el proveedor elige en el calendario
    private List<ItemConfirmacionDTO> items;

    @Data
    public static class ItemConfirmacionDTO {
        private Long productoId;
        private BigDecimal nuevoPrecio; // El precio que el proveedor edita
    }
}