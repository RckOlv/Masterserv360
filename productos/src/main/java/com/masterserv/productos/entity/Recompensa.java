package com.masterserv.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.masterserv.productos.enums.TipoDescuento;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "recompensas")
@Data
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion; // Ej: "20% OFF en Neumáticos" o "$500 de descuento"

    @Column(name = "puntos_requeridos", nullable = false)
    private int puntosRequeridos; // Ej: 150

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false)
    private TipoDescuento tipoDescuento; // FIJO o PORCENTAJE

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor; // Ej: 500 (si es FIJO) or 20 (si es PORCENTAJE)

    // Relación: Muchas recompensas pertenecen a Una Regla de Puntos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_puntos_id", nullable = false)
    @JsonBackReference
    private ReglaPuntos reglaPuntos; 

    // Relación: Una recompensa puede aplicar a UNA categoría (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = true) 
    private Categoria categoria; // Si es null, aplica a toda la compra
}