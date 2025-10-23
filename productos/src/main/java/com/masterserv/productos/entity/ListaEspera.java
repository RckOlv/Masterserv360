package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "listas_espera",
    uniqueConstraints = {
        // Clave única para que un usuario no se anote 2 veces al mismo producto
        @UniqueConstraint(columnNames = {"usuario_id", "producto_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Column(nullable = false, length = 20)
    private String estado; // Ej: "ACTIVA", "NOTIFICADA"

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;
}