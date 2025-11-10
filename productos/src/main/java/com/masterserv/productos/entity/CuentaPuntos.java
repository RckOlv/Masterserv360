package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "cuentas_puntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"cliente", "movimientos"})
public class CuentaPuntos extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Column(name = "saldo_puntos", nullable = false)
    private Integer saldoPuntos; // El nombre del campo Java 'saldoPuntos' está bien
    // --- FIN DE LA CORRECCIÓN ---

    /**
     * Relación 1:1 con el Usuario (Cliente).
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_usuario_id", nullable = false, unique = true)
    private Usuario cliente; // FK al Usuario (Cliente)

    /**
     * Relación inversa: Una Cuenta tiene MUCHOS Movimientos (Historial).
     */
    @OneToMany(mappedBy = "cuentaPuntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.Set<MovimientoPuntos> movimientos;

    // --- Métodos de Conveniencia ---
    // (Este método @PrePersist es perfecto, asegura que 'saldoPuntos' nunca sea null)
    @PrePersist
    public void prePersist() {
        if (this.saldoPuntos == null) {
            this.saldoPuntos = 0;
        }
    }
}