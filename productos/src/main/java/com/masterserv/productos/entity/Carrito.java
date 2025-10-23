package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "carritos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    // --- Relaciones ---

    // Relación 1:1 con el Vendedor (Usuario)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_usuario_id", nullable = false, unique = true)
    private Usuario vendedor;

    // Relación inversa: Un Carrito tiene MUCHOS Items
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<ItemCarrito> items;
}