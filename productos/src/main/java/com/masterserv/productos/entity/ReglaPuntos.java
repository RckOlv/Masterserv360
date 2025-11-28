package com.masterserv.productos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString; // <-- Mentor: Importar
import java.math.BigDecimal;
import java.time.LocalDate; 
import java.util.Set; // <-- Mentor: Importar Set

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.HashSet; // <-- Mentor: Importar HashSet

@Entity
@Table(name = "reglas_puntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "fechaCreacion", column = @Column(name = "fecha_inicio_vigencia"))
@ToString(exclude = "recompensas") // <-- Mentor: Añadido para evitar bucles
public class ReglaPuntos extends AuditableEntity { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "monto_gasto", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoGasto;
    
    @Column(name = "puntos_ganados", nullable = false)
    private Integer puntosGanados;

    // --- Mentor: INICIO DE LA MODIFICACIÓN ---
    // Este campo ya no define el canje, solo el valor informativo
    @Column(name = "equivalencia_puntos", nullable = true, precision = 10, scale = 2)
    private BigDecimal equivalenciaPuntos; // Se vuelve 'nullable' (opcional)

    // Nueva relación: Una regla tiene muchas recompensas
    // (CascadeType.ALL significa que si guardas/borras una Regla, se guardan/borran sus recompensas)
    @OneToMany(mappedBy = "reglaPuntos", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private Set<Recompensa> recompensas = new HashSet<>();
    // --- Mentor: FIN DE LA MODIFICACIÓN ---

    @Column(name = "estado_regla", length = 20, nullable = false)
    private String estadoRegla;

    @Column(name = "caducidad_puntos_meses")
    private Integer caducidadPuntosMeses;

    @Column(name = "vigencia_desde")
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta")
    private LocalDate vigenciaHasta;
    
    // --- Mentor: Añadir equals/hashCode (buena práctica para OneToMany) ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReglaPuntos)) return false;
        ReglaPuntos that = (ReglaPuntos) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}