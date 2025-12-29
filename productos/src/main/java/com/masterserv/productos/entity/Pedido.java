package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID; // ✅ IMPORTANTE: Faltaba este import

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_pedido", nullable = false)
    private LocalDateTime fechaPedido; 

    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoPedido estado;

    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;

    @Column(name = "total_pedido", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    @ToString.Exclude
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<DetallePedido> detalles;

    @PrePersist
    public void prePersist() {
        if (this.token == null || this.token.isEmpty()) {
            this.token = UUID.randomUUID().toString();
        }
        if (this.fechaPedido == null) {
            this.fechaPedido = LocalDateTime.now();
        }
    }
}