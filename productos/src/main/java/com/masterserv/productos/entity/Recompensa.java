package com.masterserv.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.masterserv.productos.enums.TipoDescuento;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "recompensas")
@Data
public class Recompensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mentor: Aquí guardas el nombre, ej: "Gorra Masterserv"
    @Column(nullable = false)
    private String descripcion; 

    @Column(name = "puntos_requeridos", nullable = false)
    private int puntosRequeridos; 

    // --- NUEVO CAMPO: STOCK ---
    // Agregamos esto para que el error 'getStock undefined' desaparezca
    @Column(nullable = false)
    private Integer stock; 
    // --------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false)
    private TipoDescuento tipoDescuento; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regla_puntos_id", nullable = false)
    @JsonBackReference
    private ReglaPuntos reglaPuntos; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = true) 
    private Categoria categoria; 
    
    // Mentor: Método helper para asegurar que nunca sea null al crear
    @PrePersist
    public void prePersist() {
        if (this.stock == null) this.stock = 0;
    }
}