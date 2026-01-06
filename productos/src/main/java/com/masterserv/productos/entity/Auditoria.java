package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_logs")
@Data
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entidad;
    private String entidadId;
    private String accion;       
    private String usuario;      
    private LocalDateTime fecha;
    
    @Column(columnDefinition = "TEXT")
    private String detalle; // Resumen corto

    // --- NUEVAS COLUMNAS ---
    @Column(columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(columnDefinition = "TEXT")
    private String valorNuevo;
}