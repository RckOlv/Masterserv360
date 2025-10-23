package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tipos_documento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_corto", nullable = false, unique = true, length = 10)
    private String nombreCorto;

    @Column(name = "nombre_largo", nullable = false, length = 50)
    private String nombreLargo;
}