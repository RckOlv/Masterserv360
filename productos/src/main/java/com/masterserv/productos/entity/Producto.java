package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal; // Usamos BigDecimal para dinero

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50) // El código de producto que agregaste
    private String codigo;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Lob // Indica que puede ser un texto largo
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // Usamos BigDecimal para precisión monetaria. Evita errores de redondeo de float/double
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "precio_costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioCosto;

    @Column(name = "imagen_url", length = 255) // Solo la URL de la imagen
    private String imagenUrl;

    @Column(name = "stock_actual", nullable = false)
    private int stockActual; // `int` primitivo (no puede ser NULL)

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo; // `int` primitivo (no puede ser NULL)

    @Column(length = 50)
    private String estado; // Podríamos hacer un Enum para esto más tarde si es necesario

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY) // Un producto pertenece a UNA Categoria
    @JoinColumn(name = "categoria_id", nullable = false) // La FK no puede ser nula
    private Categoria categoria;
}