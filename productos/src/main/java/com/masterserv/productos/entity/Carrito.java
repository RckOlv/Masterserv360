package com.masterserv.productos.entity;

import jakarta.persistence.*;
// --- Imports de Lombok Corregidos ---
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// ------------------------------------
// No necesitamos LocalDateTime aquí, lo hereda
import java.util.HashSet; // Importar HashSet para inicializar
import java.util.Set;

@Entity
@Table(name = "carritos")
// --- ¡CAMBIO CRÍTICO! Reemplazamos @Data ---
// @Data // ¡ELIMINADO!
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// --------------------------------------------
// --- ¡CAMBIO CRÍTICO! Añadimos la herencia ---
public class Carrito extends AuditableEntity { // <-- AHORA EXTIENDE AuditableEntity
// --------------------------------------------

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- ¡ELIMINADO! ---
    // Ya no necesitamos definir fechaCreacion aquí, lo hereda de AuditableEntity
    // @Column(name = "fecha_creacion", nullable = false)
    // private LocalDateTime fechaCreacion;
    // -------------------

    // --- Relaciones ---

    // Relación 1:1 con el Vendedor (Usuario)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_usuario_id", nullable = false, unique = true)
    @ToString.Exclude // Para evitar bucles
    private Usuario vendedor;

    // Relación inversa: Un Carrito tiene MUCHOS Items
    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude // ¡Clave para evitar StackOverflowError!
    // Inicializar la colección es buena práctica
    private Set<ItemCarrito> items = new HashSet<>();
}