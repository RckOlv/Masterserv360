package com.masterserv.productos.event;

import org.springframework.context.ApplicationEvent;

/**
 * Evento que se publica cuando una Venta se ha completado y persistido
 * exitosamente en la base de datos.
 * Llevamos el ID de la venta.
 */
public class VentaRealizadaEvent extends ApplicationEvent {

    private final Long ventaId;

    public VentaRealizadaEvent(Object source, Long ventaId) {
        super(source);
        this.ventaId = ventaId;
    }

    public Long getVentaId() {
        return ventaId;
    }
}