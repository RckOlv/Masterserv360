package com.masterserv.productos.event;

/**
 * Evento que se publica cada vez que el stock de un producto cambia.
 * Contiene lo necesario para que los oyentes tomen decisiones.
 *
 * @param productoId El ID del producto que cambió.
 * @param stockAnterior El stock que tenía ANTES del cambio.
 * @param stockNuevo El stock que tiene AHORA.
 */
public record StockActualizadoEvent(
    Long productoId,
    int stockAnterior,
    int stockNuevo
) {
}