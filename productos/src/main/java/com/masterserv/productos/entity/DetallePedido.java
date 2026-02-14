package com.masterserv.productos.entity;

import jakarta.persistence.*;
// Imports de Lombok cambiados
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "detalles_pedido")
// --- ¡CAMBIO CRÍTICO! ---
// @Data (ELIMINADO)
@Getter // Añadido
@Setter // Añadido
@NoArgsConstructor // Añadido
@AllArgsConstructor // Añadido
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
    @ToString.Exclude // ¡MUY IMPORTANTE! Evita bucles en logs y hashCode
    @JsonBackReference // Para evitar problemas de serialización JSON
    private Pedido pedido; // FK a la cabecera del pedido

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude // Para evitar bucles en logs
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