package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoVenta;
import jakarta.persistence.*;
// --- Imports de Lombok Corregidos ---
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// ------------------------------------
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "ventas")
// --- ¡CAMBIO CRÍTICO! Reemplazamos @Data ---
// @Data // ¡ELIMINADO!
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// --------------------------------------------
public class Venta extends AuditableEntity {

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

    @Column(name = "metodo_pago")
    private String metodoPago;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_usuario_id", nullable = false) // FK al Vendedor (Usuario)
    @ToString.Exclude // Para evitar bucles
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_usuario_id", nullable = false) // FK al Cliente (Usuario)
    @ToString.Exclude // Para evitar bucles
    private Usuario cliente;

    // --- Relación Inversa ---

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude // ¡Clave para evitar StackOverflowError!
    private Set<DetalleVenta> detalles;

    @OneToOne // Una Venta puede tener un Cupón
    @JoinColumn(name = "cupon_id", referencedColumnName = "id")
    private Cupon cupon;

    @Column(name = "monto_descuento", precision = 10, scale = 2)
    private BigDecimal montoDescuento;
}