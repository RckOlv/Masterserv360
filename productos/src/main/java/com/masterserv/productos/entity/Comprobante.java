package com.masterserv.productos.entity;

import com.masterserv.productos.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 50)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false, unique = true, length = 100)
    private String numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    // --- Relaciones Polimórficas (1:1 Opcional) ---

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", unique = true) // FK a Venta (anulable)
    private Venta venta;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", unique = true) // FK a Pedido (anulable)
    private Pedido pedido;
}