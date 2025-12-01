package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_productos")
@Data
public class SolicitudProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descripcion; // Ej: "Escape deportivo para Honda Titan"

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private boolean procesado; // Para que el admin marque cuando ya lo leyó/consiguió

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Constructores helpers
    public SolicitudProducto() {}

    public SolicitudProducto(String descripcion, Usuario usuario) {
        this.descripcion = descripcion;
        this.usuario = usuario;
        this.fechaSolicitud = LocalDateTime.now();
        this.procesado = false;
    }
}