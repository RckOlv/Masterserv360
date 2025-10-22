package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permisos")
@Getter
@Setter
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPermiso;

    @Column(nullable = false, unique = true)
    private String nombre; // Ej: "CREAR_PRODUCTO", "VER_VENTAS"
}
