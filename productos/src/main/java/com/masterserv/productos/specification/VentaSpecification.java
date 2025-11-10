package com.masterserv.productos.specification;

import com.masterserv.productos.dto.VentaFiltroDTO;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.entity.Usuario; // Importar Usuario para joins
import jakarta.persistence.criteria.*; // Importar clases de Criteria API
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component; // Marcar como componente si se inyecta

import java.time.LocalDateTime; // Usar LocalDateTime para comparar con fechaVenta
import java.util.ArrayList;
import java.util.List;

// No es estrictamente necesario marcarlo como @Component a menos que lo inyectes
// en otro lugar, pero es buena práctica.
@Component
public class VentaSpecification {

    /**
     * Construye una Specification<Venta> basada en los criterios del DTO.
     * Combina todos los predicados (condiciones WHERE) con AND.
     */
    public Specification<Venta> build(VentaFiltroDTO filtro) {
        return (Root<Venta> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Filtro por Cliente ---
            if (filtro.getClienteId() != null) {
                // Hacemos JOIN con la entidad Usuario (cliente)
                Join<Venta, Usuario> clienteJoin = root.join("cliente", JoinType.INNER);
                predicates.add(cb.equal(clienteJoin.get("id"), filtro.getClienteId()));
            }

            // --- Filtro por Vendedor ---
            if (filtro.getVendedorId() != null) {
                // Hacemos JOIN con la entidad Usuario (vendedor)
                Join<Venta, Usuario> vendedorJoin = root.join("vendedor", JoinType.INNER);
                predicates.add(cb.equal(vendedorJoin.get("id"), filtro.getVendedorId()));
            }

            // --- Filtro por Fecha Desde ---
            if (filtro.getFechaDesde() != null) {
                // Convertimos LocalDate (inicio del día) a LocalDateTime
                LocalDateTime fechaDesdeInicioDia = filtro.getFechaDesde().atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaVenta"), fechaDesdeInicioDia));
            }

            // --- Filtro por Fecha Hasta ---
            if (filtro.getFechaHasta() != null) {
                // Convertimos LocalDate (fin del día) a LocalDateTime
                LocalDateTime fechaHastaFinDia = filtro.getFechaHasta().plusDays(1).atStartOfDay(); // Hasta el inicio del día SIGUIENTE
                predicates.add(cb.lessThan(root.get("fechaVenta"), fechaHastaFinDia)); // Usamos lessThan para no incluir el día siguiente
            }

            // --- Filtro por Estado ---
            if (filtro.getEstado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.getEstado()));
            }

            // Combina todos los predicados con AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}