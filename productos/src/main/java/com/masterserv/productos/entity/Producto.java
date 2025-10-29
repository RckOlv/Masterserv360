package com.masterserv.productos.entity;

import jakarta.persistence.*;
// --- ¡CAMBIO CRÍTICO DE LOMBOK! ---
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// ------------------------------------
import java.math.BigDecimal; 

@Entity
@Table(name = "productos")
// --- ¡CAMBIO CRÍTICO! Reemplazamos @Data ---
// @Data // ¡ELIMINADO!
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// --------------------------------------------
public class Producto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "productos_id_generator")
    @SequenceGenerator(name = "productos_id_generator", sequenceName = "productos_id_seq", allocationSize = 1)
    @Column(name = "id") 
    private Long id; 

    // ... (codigo, nombre, descripcion, precioVenta, precioCosto, etc.) ...
    @Column(name = "codigo", unique = true, length = 50) 
    private String codigo; 

    @Column(name = "nombre", nullable = false, length = 255) 
    private String nombre; 

    @Column(name = "descripcion") 
    private String descripcion; 

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2) 
    private BigDecimal precioVenta;

    @Column(name = "precio_costo", nullable = false, precision = 10, scale = 2) 
    private BigDecimal precioCosto;

    @Column(name = "imagen_url", length = 255) 
    private String imagenUrl;

    @Column(name = "stock_actual", nullable = false) 
    private int stockActual; 

    @Column(name = "stock_minimo", nullable = false) 
    private int stockMinimo; 

    @Column(name = "estado", length = 50) 
    private String estado; 

    // --- Relación ---
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "categoria_id", nullable = false) 
    @ToString.Exclude // Para evitar bucles
    private Categoria categoria;
    
    // --- ¡ELIMINADO! ---
    // El Set<Proveedor> que te pedí añadir era incorrecto. Lo quitamos.
}