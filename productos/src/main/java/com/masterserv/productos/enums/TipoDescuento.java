package com.masterserv.productos.enums;

/**
 * Define si un cupón aplica un monto fijo (ARS) o un porcentaje (%).
 */
public enum TipoDescuento {
    /**
     * Un descuento de monto fijo (Ej: $500 ARS).
     */
    FIJO,
    
    /**
     * Un descuento porcentual (Ej: 15%).
     */
    PORCENTAJE
}