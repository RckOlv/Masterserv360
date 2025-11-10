package com.masterserv.productos.enums;

/**
 * Define los tipos de transacciones de puntos de fidelización.
 */
public enum TipoMovimientoPuntos {
    GANADO,     // Puntos obtenidos por una compra
    CANJEADO,   // Puntos utilizados para un descuento/premio
    EXPIRADO,   // Puntos caducados por el tiempo
    REVERSION, // Puntos devueltos por una cancelación o devolución
    AJUSTE      // Ajuste manual por el administrador
}