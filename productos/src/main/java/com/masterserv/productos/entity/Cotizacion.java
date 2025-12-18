package com.masterserv.productos.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference; // <--- IMPORTANTE
import com.masterserv.productos.enums.EstadoCotizacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString; // <--- IMPORTANTE

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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
    private Proveedor proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoCotizacion estado;

    @Column(unique = true, nullable = false, length = 36)
    private String token;

    @Column(name = "fecha_entrega_ofertada")
    private LocalDate fechaEntregaOfertada;

    @Column(name = "precio_total_ofertado", precision = 10, scale = 2)
    private BigDecimal precioTotalOfertado;

    @Column(name = "es_recomendada")
    private boolean esRecomendada = false;

    // --- CORRECCIÓN AQUÍ ---
    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // <--- 1. Rompe el ciclo en JSON (Lado Padre)
    @ToString.Exclude     // <--- 2. Evita recursión si alguna vez usas toString()
    private Set<ItemCotizacion> items = new HashSet<>();
}