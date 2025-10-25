package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoVenta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoVenta estado;

    @Column(name = "total_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalVenta;

    // --- Relaciones (Las FKs de Rol que discutimos) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_usuario_id", nullable = false) // FK al Vendedor (Usuario)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_usuario_id", nullable = false) // FK al Cliente (Usuario)
    private Usuario cliente;

    // --- Relación Inversa ---
    
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DetalleVenta> detalles;
}