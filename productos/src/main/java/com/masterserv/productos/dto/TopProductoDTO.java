package com.masterserv.productos.dto;

// (No necesita imports si usas 'record')

/**
 * DTO para transportar los productos más vendidos.
 * 'record' es una forma moderna de Java para crear clases de datos inmutables.
 */
public record TopProductoDTO(
    Long productoId,
    String nombre,
    Long cantidadVendida // El SUM(cantidad) de SQL devuelve un Long
) {
}