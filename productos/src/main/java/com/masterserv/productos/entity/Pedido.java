package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoPedido;
import jakarta.persistence.*;
// Imports de Lombok cambiados
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "pedidos")
// --- ¡CAMBIO CRÍTICO! ---
// @Data (ELIMINADO)
@Getter // Añadido
@Setter // Añadido
@NoArgsConstructor // Añadido
@AllArgsConstructor // Añadido
// -------------------------
public class Pedido extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoPedido estado;

    @Column(name = "total_pedido", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPedido;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false) // FK a Proveedor
    @ToString.Exclude // Para evitar bucles en logs
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false) // FK al Usuario (empleado) que creó el pedido
    @ToString.Exclude // Para evitar bucles en logs
    private Usuario usuario;

    // Relación inversa: Un Pedido tiene MUCHOS Detalles
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // ¡MUY IMPORTANTE! Evita bucles en logs
    private Set<DetallePedido> detalles;
}