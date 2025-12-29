package com.masterserv.productos.entity;

import jakarta.persistence.*;
// --- Imports de Lombok Corregidos ---
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
// ------------------------------------
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_venta")
// --- ¡CAMBIO CRÍTICO! Reemplazamos @Data ---
// @Data // ¡ELIMINADO!
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// --------------------------------------------
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int cantidad;

    // Precio "congelado" al momento de la venta
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false) // FK a la cabecera de la Venta
    @JsonIgnore // Evita serialización infinita
    @ToString.Exclude // ¡Clave para evitar StackOverflowError!
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false) // FK al producto vendido
    @ToString.Exclude // Para evitar bucles
    private Producto producto;

    // Nota: El método @Transient getSubtotal() se puede añadir aquí si lo necesitas,
    //       igual que lo tenías en DetallePedido.java. Por ahora lo omito
    //       para mantenerlo simple, pero es buena idea tenerlo.
}