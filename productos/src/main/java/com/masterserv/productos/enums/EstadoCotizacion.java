package com.masterserv.productos.enums;

/**
 * Define los estados posibles de una Cotizacion (Pre-Pedido) completa
 * enviada a un proveedor.
 */
public enum EstadoCotizacion {

    /**
     * El sistema ha generado la cotización y la ha enviado al proveedor
     * (vía email/link). Estamos esperando que el proveedor ingrese sus precios.
     */
    PENDIENTE_PROVEEDOR,

    /**
     * El proveedor ha completado el formulario del link, ha ingresado
     * sus precios/fechas y ha presionado "Enviar". La oferta ya está
     * en nuestro sistema, lista para ser evaluada por el Administrador.
     */
    RECIBIDA,

    /**
     * El Administrador ha revisado las ofertas recibidas y ha
     * seleccionado esta cotización como la ganadora.
     * Esto dispara la creación de un 'Pedido' formal.
     */
    CONFIRMADA_ADMIN,

    /**
     * El Administrador ha cancelado esta cotización completa (no solo un item).
     * (Ej. ya no necesita los productos o eligió otra cotización).
     */
    CANCELADA_ADMIN,

    /**
     * El token del link ha expirado (ej. 3 días) y el proveedor
     * nunca respondió. El sistema la marca como vencida.
     */
    VENCIDA
}