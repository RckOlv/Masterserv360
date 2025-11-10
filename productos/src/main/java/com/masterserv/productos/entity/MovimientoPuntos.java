package com.masterserv.productos.entity;

import com.masterserv.productos.enums.TipoMovimientoPuntos;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_puntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString // ✅ Se eliminó el parámetro 'exclude'
public class MovimientoPuntos extends AuditableEntity { // Hereda fechaCreacion/fechaModificacion

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Puntos que ingresan (positivo) o salen (negativo).
     */
    @Column(nullable = false)
    private Integer puntos;

    /**
     * Tipo de transacción: GANADO, CANJEADO, EXPIRADO, AJUSTE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimientoPuntos tipoMovimiento;

    /**
     * Fecha en que caducarán estos puntos específicos (si la regla lo define).
     */
    @Column(name = "fecha_caducidad_puntos")
    private LocalDateTime fechaCaducidadPuntos;

    /**
     * Descripción o referencia del movimiento.
     */
    @Column(length = 255)
    private String descripcion;

    // --- Relaciones ---

    // FK a la Cuenta de Puntos (relación bidireccional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_puntos_id", nullable = false)
    @ToString.Exclude // ✅ Correcto uso del nuevo estilo
    private CuentaPuntos cuentaPuntos;

    // FK a la Venta que generó los puntos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id") // No es nullable, ya que no todos los movimientos son por venta (ej. ajuste)
    @ToString.Exclude // ✅ Correcto uso del nuevo estilo
    private Venta venta;
}
