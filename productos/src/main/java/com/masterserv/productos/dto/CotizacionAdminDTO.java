package com.masterserv.productos.dto;

import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.enums.EstadoCotizacion;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class CotizacionAdminDTO {
    
    private Long id;
    private String proveedorNombre;
    private Long proveedorId;
    private EstadoCotizacion estado;
    private LocalDateTime fechaCreacion;
    private LocalDate fechaEntregaOfertada;
    private BigDecimal precioTotalOfertado;
    private boolean esRecomendada;
    private Set<ItemCotizacionAdminDTO> items;

    // Constructor para el mapeo
    public CotizacionAdminDTO(Cotizacion cotizacion) {
        this.id = cotizacion.getId();
        this.proveedorNombre = cotizacion.getProveedor().getRazonSocial();
        this.proveedorId = cotizacion.getProveedor().getId();
        this.estado = cotizacion.getEstado();
        this.fechaCreacion = cotizacion.getFechaCreacion();
        this.fechaEntregaOfertada = cotizacion.getFechaEntregaOfertada();
        this.precioTotalOfertado = cotizacion.getPrecioTotalOfertado();
        this.esRecomendada = cotizacion.isEsRecomendada();
        this.items = cotizacion.getItems().stream()
            .map(ItemCotizacionAdminDTO::new)
            .collect(Collectors.toSet());
    }
}