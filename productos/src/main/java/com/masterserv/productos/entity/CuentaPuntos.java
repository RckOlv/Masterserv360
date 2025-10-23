package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cuentas_puntos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saldo_actual", nullable = false)
    private int saldoActual;

    // --- Relación 1:1 ---
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_usuario_id", nullable = false, unique = true)
    private Usuario cliente;
}