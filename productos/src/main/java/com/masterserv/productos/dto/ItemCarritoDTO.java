package com.masterserv.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCarritoDTO {

    // --- Campos de Lectura (Para mostrar en el frontend) ---
    private Long id; // ID del ItemCarrito (para poder quitarlo/actualizarlo)
    private Long productoId;
    private String productoNombre;
    private String productoCodigo;
    private BigDecimal precioUnitarioVenta; // Precio de venta del producto
    private int cantidad;
    private BigDecimal subtotal; // Calculado (precio * cantidad)
    private int stockDisponible; // ¡Útil! Para mostrar al vendedor si hay suficiente
    private Long productoCategoriaId;

}