package com.masterserv.productos.dto;

import com.masterserv.productos.entity.ItemCotizacion;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO público que ve el proveedor para CADA item que le pedimos.
 */
@Data
public class ItemCotizacionPublicoDTO {
    
    private Long id; // El ID del ItemCotizacion (para la respuesta)
    private String productoNombre;
    private String productoCodigo;
    private int cantidadSolicitada;
    
    // Este campo lo llenará el proveedor en su formulario
    private BigDecimal precioUnitarioOfertado; // Vendrá null
    
    // Constructor simple para el Mapeo
    public ItemCotizacionPublicoDTO(ItemCotizacion item) {
        this.id = item.getId();
        this.productoNombre = item.getProducto().getNombre();
        this.productoCodigo = item.getProducto().getCodigo();
        this.cantidadSolicitada = item.getCantidadSolicitada();
        this.precioUnitarioOfertado = null; // Siempre se envía null para que él lo llene
    }
}