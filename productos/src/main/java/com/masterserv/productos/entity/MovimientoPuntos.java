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
@ToString 
public class MovimientoPuntos extends AuditableEntity { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer puntos;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimientoPuntos tipoMovimiento;

    @Column(name = "fecha_caducidad_puntos")
    private LocalDateTime fechaCaducidadPuntos;

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
    @JoinColumn(name = "venta_id") 
    @ToString.Exclude 
    private Venta venta;
}
