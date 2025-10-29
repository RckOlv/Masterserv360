package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// --- CORRECCIÓN: Quitamos "productos" del exclude ---
@ToString(exclude = "categorias") // 👈 Solo excluimos categorias
public class Proveedor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ... (razonSocial, cuit, email, telefono, etc.) ...
    @Column(nullable = false, unique = true, length = 255)
    private String razonSocial;
    // ... (resto de tus campos) ...
    @Column(nullable = false, unique = true, length = 20)
    private String cuit;
    @Column(length = 100)
    private String email;
    @Column(length = 20)
    private String telefono;
    @Column(length = 255)
    private String direccion;
    @Column(length = 50)
    private String estado;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "proveedores_categorias",
        joinColumns = @JoinColumn(name = "proveedor_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();

    // --- ¡ELIMINADO! ---
    // El Set<Producto> que te pedí añadir era incorrecto. Lo quitamos.

    // ... (Tu equals y hashCode están perfectos) ...
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proveedor)) return false;
        Proveedor that = (Proveedor) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}