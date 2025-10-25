package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "permisos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permiso extends AuditableEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_permiso", nullable = false, unique = true, length = 50)
    private String nombrePermiso;

    @Column(length = 255)
    private String descripcion;

    // --- Relaciones ---
    // Esta entidad no "posee" relaciones directas, 
    // será referenciada por la tabla de unión 'roles_permisos'.
    // Así que no necesitamos @OneToMany ni @ManyToMany aquí.
}