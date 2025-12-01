package com.masterserv.productos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

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

    @NotNull(message = "El lote de reposición es obligatorio")
    @Min(value = 1, message = "El lote de reposición debe ser al menos 1")
    int loteReposicion,

    String estado,

    @NotNull(message = "La categoría es obligatoria")
    Long categoriaId, 

    String categoriaNombre,
    
    // --- MENTOR: CAMPO NUEVO AGREGADO AL RECORD ---
    Long solicitudId 
    // ----------------------------------------------
) {
    
    // Constructor customizado (Actualizado para recibir solicitudId)
    public ProductoDTO(Long id, String codigo, String nombre, String descripcion, 
                       BigDecimal precioVenta, BigDecimal precioCosto, String imagenUrl, 
                       int stockActual, int stockMinimo, int loteReposicion, 
                       String estado, Long categoriaId, String categoriaNombre,
                       Long solicitudId) { // <-- Nuevo parámetro
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.precioCosto = precioCosto;
        this.imagenUrl = imagenUrl;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.loteReposicion = loteReposicion;
        this.estado = estado;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.solicitudId = solicitudId; // <-- Asignación
    }

    // Constructor auxiliar (Actualizado para pasar null al nuevo campo)
    public ProductoDTO(Long id, String codigo, String nombre, String descripcion, 
                       BigDecimal precioVenta, BigDecimal precioCosto, String imagenUrl, 
                       int stockActual, int stockMinimo, int loteReposicion, 
                       String estado, Long categoriaId) {
        // Llamamos al constructor principal pasando 'null' en los campos extra
        this(id, codigo, nombre, descripcion, precioVenta, precioCosto, imagenUrl, 
             stockActual, stockMinimo, loteReposicion, estado, categoriaId, null, null); 
             // El último null es para solicitudId
    }
    
    // --- MENTOR: GETTER MANUAL (Opcional en records, pero útil para compatibilidad) ---
    // Los records ya tienen el método .solicitudId(), pero si tu código busca .getSolicitudId()
    // puedes agregar esto para que no falle nada:
    public Long getSolicitudId() {
        return solicitudId;
    }
}