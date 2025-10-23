package com.masterserv.productos.enums;

public enum TipoMovimientoPuntos {
    ACUMULACION, // Ganar puntos (por venta o ajuste)
    CANJE,       // Gastar puntos (para generar un cupón)
    AJUSTE_MANUAL, // Corrección de admin
    VENCIMIENTO  // Puntos que expiran
}