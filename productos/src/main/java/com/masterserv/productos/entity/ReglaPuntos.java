package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "reglas_puntos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReglaPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(name = "equivalencia_monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal equivalenciaMonto; // Ej: 1000 (pesos)

    @Column(name = "equivalencia_puntos", nullable = false)
    private int equivalenciaPuntos; // Ej: 10 (puntos)

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;
}