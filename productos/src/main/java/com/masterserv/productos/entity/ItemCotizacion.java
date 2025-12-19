package com.masterserv.productos.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Importante
import com.masterserv.productos.enums.EstadoItemCotizacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "items_cotizacion")
@Getter
@Setter
public class ItemCotizacion extends AuditableEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    @JsonBackReference 
    @ToString.Exclude  
    private Cotizacion cotizacion; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    // Evita bucles y carga pesada de datos innecesarios en el JSON
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "fechaCreacion", "fechaModificacion", "categoria", "descripcion", "imagenUrl", "stockActual", "stockMinimo", "loteReposicion", "precioCosto", "precioVenta"})
    private Producto producto;

    @Column(name = "cantidad_solicitada", nullable = false)
    private int cantidadSolicitada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoItemCotizacion estado;

    @Column(name = "precio_unitario_ofertado", precision = 10, scale = 2)
    private BigDecimal precioUnitarioOfertado;
}