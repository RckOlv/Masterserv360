package com.masterserv.productos.dto;

import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ItemCotizacionAdminDTO {
    
    private Long id; // ID del ItemCotizacion
    private Long productoId;
    private String productoNombre;
    private int cantidadSolicitada;
    private EstadoItemCotizacion estado;
    private BigDecimal precioUnitarioOfertado;
    private BigDecimal subtotalOfertado;

    // Constructor para el mapeo
    public ItemCotizacionAdminDTO(ItemCotizacion item) {
        this.id = item.getId();
        this.productoId = item.getProducto().getId();
        this.productoNombre = item.getProducto().getNombre();
        this.cantidadSolicitada = item.getCantidadSolicitada();
        this.estado = item.getEstado();
        this.precioUnitarioOfertado = item.getPrecioUnitarioOfertado();
        
        // Calculamos el subtotal (si ya fue cotizado)
        if (this.precioUnitarioOfertado != null) {
            this.subtotalOfertado = this.precioUnitarioOfertado.multiply(
                new BigDecimal(this.cantidadSolicitada)
            );
        } else {
            this.subtotalOfertado = BigDecimal.ZERO;
        }
    }
}