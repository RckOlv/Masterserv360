package com.masterserv.productos.entity;

import com.masterserv.productos.enums.TipoMovimiento;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 50)
    private TipoMovimiento tipoMovimiento;

    @Column(nullable = false)
    private int cantidad; // Cantidad que cambió (puede ser positiva o negativa)

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 255)
    private String motivo;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false) // El usuario que registró el movimiento
    private Usuario usuario;
    
    // NOTA: Faltarían FKs a 'venta_id' o 'pedido_id' para trazarlo
    // automáticamente, pero por velocidad lo dejamos así. El 'motivo' y 'tipo'
    // nos darán la pista.
}