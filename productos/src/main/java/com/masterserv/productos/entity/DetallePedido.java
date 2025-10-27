package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int cantidad;

    // Precio unitario congelado en el momento del pedido
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido; // FK a la cabecera del pedido

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto; // FK al producto vendido

    // --- Métodos de conveniencia ---

    @Transient
    public BigDecimal getSubtotal() {
        if (precioUnitario != null) {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
        return BigDecimal.ZERO;
    }
}
