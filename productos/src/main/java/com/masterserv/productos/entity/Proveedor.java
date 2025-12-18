package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoUsuario; // <--- 1. Importar el Enum
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
@ToString(exclude = "categorias") 
public class Proveedor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String razonSocial;

    @Column(nullable = false, unique = true, length = 20)
    private String cuit;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    // --- CORRECCIÓN CRÍTICA AQUÍ ---
    // Cambiamos String por el Enum.
    // @Enumerated(EnumType.STRING) le dice a JPA: "Guarda el texto 'ACTIVO' en la base de datos, pero en Java úsalo como Enum".
    @Enumerated(EnumType.STRING) 
    @Column(length = 50)
    private EstadoUsuario estado; 

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "proveedores_categorias",
        joinColumns = @JoinColumn(name = "proveedor_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();

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