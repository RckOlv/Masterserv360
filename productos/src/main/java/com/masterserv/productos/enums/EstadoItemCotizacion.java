package com.masterserv.productos.enums;

/**
 * Define los estados posibles de un item *dentro* de una Cotizacion.
 * Actualizado para soportar lógica de competencia y cierre automático.
 */
public enum EstadoItemCotizacion {

    /**
     * Estado inicial. Se ha solicitado cotización, el proveedor aún no responde.
     */
    PENDIENTE,

    /**
     * El proveedor ingresó precio y stock. Es candidato a ser elegido.
     */
    COTIZADO,

    /**
     * El proveedor indicó explícitamente que NO tiene stock.
     */
    NO_DISPONIBLE_PROVEEDOR,

    /**
     * ¡GANADOR! Este item fue seleccionado por el Admin y convertido en Pedido.
     */
    CONFIRMADO,

    /**
     * El Administrador canceló manualmente este item específico.
     */
    CANCELADO_ADMIN,

    /**
     * El sistema canceló este item automáticamente porque se compró el producto
     * en otra cotización (evita sobre-stock).
     */
    CANCELADO_SISTEMA,

    
    COMPLETADO
}