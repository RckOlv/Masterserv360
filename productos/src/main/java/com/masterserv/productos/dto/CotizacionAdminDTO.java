package com.masterserv.productos.dto;

import com.masterserv.productos.entity.Cotizacion;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
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
    // --- MENTOR: NUEVO CAMPO DE ANÁLISIS ---
    private String observacionAnalisis; 
    // ---------------------------------------
    private Set<ItemCotizacionAdminDTO> items;

    public CotizacionAdminDTO(Cotizacion cotizacion) {
        this.id = cotizacion.getId();
        this.proveedorNombre = cotizacion.getProveedor().getRazonSocial();
        this.proveedorId = cotizacion.getProveedor().getId();
        this.estado = cotizacion.getEstado();
        this.fechaCreacion = cotizacion.getFechaCreacion();
        this.fechaEntregaOfertada = cotizacion.getFechaEntregaOfertada();
        this.precioTotalOfertado = cotizacion.getPrecioTotalOfertado();
        this.esRecomendada = cotizacion.isEsRecomendada();
        
        // Calculamos el análisis en tiempo real
        this.observacionAnalisis = calcularObservacion(cotizacion);

        this.items = cotizacion.getItems().stream()
            .map(ItemCotizacionAdminDTO::new)
            .collect(Collectors.toSet());
    }

    private String calcularObservacion(Cotizacion c) {
        long itemsCotizados = c.getItems().stream()
             .filter(i -> i.getEstado() == EstadoItemCotizacion.COTIZADO).count();
        int totalItems = c.getItems().size();

        // 1. Check Completeness
        if (itemsCotizados < totalItems) {
            return "⚠️ Oferta Incompleta (" + itemsCotizados + "/" + totalItems + " items)";
        }
        
        // 2. Check Winner
        if (c.isEsRecomendada()) {
            return "⭐ Mejor Opción Global";
        }
        
        // 3. Losers (Generic Reason)
        return "❌ Precio o Fecha superior";
    }
}