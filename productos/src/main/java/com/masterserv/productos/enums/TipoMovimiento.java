package com.masterserv.productos.enums;

/**
 * Define los tipos de movimientos de stock para auditoría.
 * Mapeado a un VARCHAR en la BD.
 */
public enum TipoMovimiento {
    ENTRADA_PEDIDO, // Ingreso por compra a proveedor
    SALIDA_VENTA,   // Salida por venta a cliente
    AJUSTE_MANUAL,  // Corrección de stock (positiva o negativa)
    DEVOLUCION,     // Devolución de cliente
    CARGA_INICIAL   // Primera carga de stock
}