package com.masterserv.productos.dto;

import com.masterserv.productos.entity.Cotizacion;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO público que representa la solicitud de cotización completa
 * que ve el proveedor.
 */
@Data
public class CotizacionPublicaDTO {

    private String token; // El token (para que el frontend lo guarde)
    private String proveedorNombre;
    private LocalDate fechaSolicitud;
    private List<ItemCotizacionPublicoDTO> items;
    
    // Este campo lo llenará el proveedor en su formulario
    private LocalDate fechaEntregaOfertada; // Vendrá null
    
    // Constructor simple para el Mapeo
    public CotizacionPublicaDTO(Cotizacion cotizacion) {
        this.token = cotizacion.getToken();
        this.proveedorNombre = cotizacion.getProveedor().getRazonSocial();
        this.fechaSolicitud = cotizacion.getFechaCreacion().toLocalDate();
        this.items = cotizacion.getItems().stream()
            // Filtramos por si el Admin ya canceló un item
            .filter(item -> item.getEstado() == com.masterserv.productos.enums.EstadoItemCotizacion.PENDIENTE)
            .map(ItemCotizacionPublicoDTO::new)
            .collect(Collectors.toList());
        this.fechaEntregaOfertada = null; // Siempre se envía null
    }
}