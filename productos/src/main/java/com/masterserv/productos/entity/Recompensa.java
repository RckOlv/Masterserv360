package com.masterserv.productos.entity;

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

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "puntos_requeridos", nullable = false)
    private int puntosRequeridos;

    @Column(nullable = false)
    private Integer stock;
    
    @Column(nullable = false)
    private Boolean activo = true; 

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false)
    private TipoDescuento tipoDescuento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = true)
    private Categoria categoria;

    @PrePersist
    public void prePersist() {
        if (this.stock == null) this.stock = 0;
        if (this.activo == null) this.activo = true;
    }
}