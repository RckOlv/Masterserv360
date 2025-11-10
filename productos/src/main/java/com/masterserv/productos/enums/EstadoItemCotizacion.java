package com.masterserv.productos.enums;

/**
 * Define los estados posibles de un item *dentro* de una Cotizacion.
 * Esto permite al Administrador cancelar items individuales de un pre-pedido.
 */
public enum EstadoItemCotizacion {

    /**
     * Estado inicial. Se ha solicitado cotización para este item,
     * pero el proveedor aún no ha respondido.
     */
    PENDIENTE,

    /**
     * El proveedor ha visto la solicitud y ha ingresado un precio
     * (un precioCosto) para este item.
     */
    COTIZADO,

    /**
     * El proveedor ha indicado que NO puede suministrar este item
     * (ej. no tiene stock, descontinuado).
     */
    NO_DISPONIBLE_PROVEEDOR,

    /**
     * El Administrador ha cancelado manualmente *este item* del pre-pedido,
     * (aunque el resto de la cotización pueda seguir activa).
     */
    CANCELADO_ADMIN
}