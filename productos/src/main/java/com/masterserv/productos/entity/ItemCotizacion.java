package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoItemCotizacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representa una línea de producto individual dentro de una Cotizacion.
 * Permite al Admin cancelar items específicos.
 */
@Entity
@Table(name = "items_cotizacion")
@Getter
@Setter
public class ItemCotizacion extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion; // A qué cotización padre pertenece

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto; // El producto que pedimos (ej. "Filtro de Aire")

    @Column(name = "cantidad_solicitada", nullable = false)
    private int cantidadSolicitada; // Ej. "50 unidades"

    /**
     * ¡CLAVE! El estado del ITEM, no de la cotización entera.
     * Permite al Admin cancelar solo esta línea.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoItemCotizacion estado; // PENDIENTE, COTIZADO, CANCELADO_ADMIN

    @Column(name = "precio_unitario_ofertado", precision = 10, scale = 2)
    private BigDecimal precioUnitarioOfertado; // El precio (costo) que el proveedor llenó
}