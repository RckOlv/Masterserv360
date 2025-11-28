package com.masterserv.productos.entity;

import com.masterserv.productos.enums.EstadoCupon;
import com.masterserv.productos.enums.TipoDescuento; // <-- Mentor: IMPORTADO
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cupones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cupon extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // Renombramos 'descuento' por 'valor' para que sea genérico
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor; // Puede ser $500 o 20 (de 20%)

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_descuento", nullable = false, length = 20)
    private TipoDescuento tipoDescuento; // FIJO o PORCENTAJE
    // --- Mentor: FIN DE LA MODIFICACIÓN ---

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCupon estado;

    // --- Relaciones ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_usuario_id", nullable = false) // El "dueño" del cupón
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id") // La venta donde se USÓ (anulable)
    private Venta venta;

    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // Nueva relación opcional: ¿A qué categoría aplica este cupón?
    // Si es null, aplica a toda la compra.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = true) 
    private Categoria categoria; // <-- AÑADIDO
    // --- Mentor: FIN DE LA MODIFICACIÓN ---
}