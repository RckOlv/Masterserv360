package com.masterserv.productos.entity; // Cambia esto por tu paquete real

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cajas")
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el usuario que abrió la caja (El Cajero)
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario; // Asegúrate de importar tu clase Usuario

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(name = "monto_inicial", nullable = false)
    private BigDecimal montoInicial = BigDecimal.ZERO; // Cambio para dar vuelto al abrir

    // --- ACUMULADORES DE VENTAS DURANTE EL DÍA ---
    @Column(name = "ventas_efectivo")
    private BigDecimal ventasEfectivo = BigDecimal.ZERO;

    @Column(name = "ventas_tarjeta")
    private BigDecimal ventasTarjeta = BigDecimal.ZERO;

    @Column(name = "ventas_transferencia")
    private BigDecimal ventasTransferencia = BigDecimal.ZERO;

    // --- ARQUEO Y CIERRE ---
    @Column(name = "monto_declarado")
    private BigDecimal montoDeclarado; // Lo que el cajero cuenta físicamente al cerrar

    @Column(name = "diferencia")
    private BigDecimal diferencia; // Sobrante (+) o Faltante (-)

    @Column(nullable = false)
    private String estado; // "ABIERTA" o "CERRADA"

	@Column(name = "extracciones")
    private BigDecimal extracciones = BigDecimal.ZERO;

    @PrePersist
    public void prePersist() {
        if (this.fechaApertura == null) {
            this.fechaApertura = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = "ABIERTA";
        }
    }
}