package com.masterserv.productos.enums;

/**
 * Define los estados posibles de una Cotizacion (Pre-Pedido).
 */
public enum EstadoCotizacion {

    /**
     * Esperando que el proveedor ingrese sus precios.
     */
    PENDIENTE_PROVEEDOR,

    /**
     * El proveedor envió su oferta. Lista para evaluar.
     */
    RECIBIDA,

    /**
     * El Admin la eligió como ganadora. Se generó un Pedido.
     */
    CONFIRMADA_ADMIN,

    /**
     * El Admin la canceló manualmente.
     */
    CANCELADA_ADMIN,

    /**
     * Token expirado.
     */
    VENCIDA,

    /**
     * NUEVO: El sistema la cerró automáticamente porque todos sus items
     * fueron cancelados (se compraron en otras cotizaciones).
     */
    CANCELADA_SISTEMA
}