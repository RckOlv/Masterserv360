package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate; // Importar LocalDate para campos 'date'

@Entity
@Table(name = "reglas_puntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// Esto ahora mapea 'fechaCreacion' a 'fecha_inicio_vigencia' (¡que ahora es NOT NULL!)
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_inicio_vigencia"))
public class ReglaPuntos extends AuditableEntity { // Hereda fecha_inicio_vigencia y fecha_modificacion

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // (Tu error anterior, ahora corregido)
    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    // --- ¡MAPEO CORREGIDO! ---
    // Tu DTO envía 'montoGasto'. Tu BD (después del SQL) 
    // ahora tiene una columna 'monto_gasto'. Esto los conecta.
    @Column(name = "monto_gasto", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoGasto;
    // --- FIN CORRECCIÓN ---
    
    @Column(name = "puntos_ganados", nullable = false)
    private Integer puntosGanados;

    @Column(name = "equivalencia_puntos", nullable = false, precision = 10, scale = 2)
    private BigDecimal equivalenciaPuntos;

    @Column(name = "estado_regla", length = 20, nullable = false)
    private String estadoRegla;

    @Column(name = "caducidad_puntos_meses")
    private Integer caducidadPuntosMeses;

    // --- Campos Opcionales (Mapeados para Coherencia) ---
    // El tipo 'date' de SQL mapea a 'LocalDate' de Java
    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;
}