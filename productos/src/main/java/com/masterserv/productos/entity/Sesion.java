package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones")
@Getter
@Setter
public class Sesion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSesion;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    private String token; // útil si usás JWT
}