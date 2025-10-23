package com.masterserv.productos.entity;

import com.masterserv.productos.enums.TipoMovimientoPuntos;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_puntos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimientoPuntos tipo;

    @Column(nullable = false)
    private int puntos; // Positivo para acumular, negativo para canjear

    @Column(nullable = false)
    private LocalDateTime fecha;

    // --- Relaciones (El "Hub") ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_puntos_id", nullable = false) // A qué cuenta pertenece
    private CuentaPuntos cuentaPuntos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id") // Qué venta generó los puntos (anulable)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupon_id") // Qué cupón generó el canje (anulable)
    private Cupon cupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_punto_id") // Qué regla se usó (anulable)
    private ReglaPuntos reglaPuntos;
}