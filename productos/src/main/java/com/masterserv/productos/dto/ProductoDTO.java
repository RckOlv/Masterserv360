package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * DTO para crear, actualizar y mostrar Productos.
 * Usamos un record para inmutabilidad y concisión.
 */
public record ProductoDTO(
    Long id,

    @NotEmpty(message = "El código no puede estar vacío")
    @Size(max = 50, message = "El código no puede tener más de 50 caracteres")
    String codigo,

    @NotEmpty(message = "El nombre no puede estar vacío")
    @Size(max = 255, message = "El nombre no puede tener más de 255 caracteres")
    String nombre,

    String descripcion,

    @NotNull(message = "El precio de venta es obligatorio")
    @Min(value = 0, message = "El precio de venta no puede ser negativo")
    BigDecimal precioVenta,

    @NotNull(message = "El precio de costo es obligatorio")
    @Min(value = 0, message = "El precio de costo no puede ser negativo")
    BigDecimal precioCosto,

    String imagenUrl,

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    int stockActual,

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    int stockMinimo,

    String estado,

    @NotNull(message = "La categoría es obligatoria")
    Long categoriaId, // Solo el ID de la categoría

    String categoriaNombre // Campo de solo lectura para mostrar en el frontend
) {
    // Constructor customizado para cuando solo queremos mostrar (no crear/actualizar)
    // El mapper usará este.
    public ProductoDTO(Long id, String codigo, String nombre, String descripcion, 
                       BigDecimal precioVenta, BigDecimal precioCosto, String imagenUrl, 
                       int stockActual, int stockMinimo, String estado, Long categoriaId, String categoriaNombre) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.precioCosto = precioCosto;
        this.imagenUrl = imagenUrl;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.estado = estado;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
    }

    // Constructor para creación/actualización (sin el nombre de categoría)
    public ProductoDTO(Long id, String codigo, String nombre, String descripcion, 
                       BigDecimal precioVenta, BigDecimal precioCosto, String imagenUrl, 
                       int stockActual, int stockMinimo, String estado, Long categoriaId) {
        this(id, codigo, nombre, descripcion, precioVenta, precioCosto, imagenUrl, 
             stockActual, stockMinimo, estado, categoriaId, null);
    }
}