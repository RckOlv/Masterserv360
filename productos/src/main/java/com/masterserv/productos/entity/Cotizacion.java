package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoCotizacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa la solicitud de cotización (Pre-Pedido) que se envía
 * a UN proveedor, la cual puede contener MÚLTIPLES items.
 */
@Entity
@Table(name = "cotizaciones")
@Getter
@Setter
public class Cotizacion extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor; // El proveedor al que le pedimos esto

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoCotizacion estado; // PENDIENTE_PROVEEDOR, RECIBIDA, CONFIRMADA, etc.

    @Column(unique = true, nullable = false, length = 36)
    private String token; // El link secreto (UUID) para este proveedor

    @Column(name = "fecha_entrega_ofertada")
    private LocalDate fechaEntregaOfertada; // La fecha que el proveedor promete

    @Column(name = "precio_total_ofertado", precision = 10, scale = 2)
    private BigDecimal precioTotalOfertado; // La suma de los items que el proveedor cotizó

    @Column(name = "es_recomendada")
    private boolean esRecomendada = false; // true si nuestro sistema la marca como la mejor opción

    /**
     * ¡CLAVE! Una cotización tiene MUCHOS items.
     * CascadeType.ALL: Si borramos la cotización, se borran sus items.
     */
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ItemCotizacion> items = new HashSet<>();
}