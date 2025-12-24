package com.masterserv.productos.specification;

import com.masterserv.productos.dto.AuditoriaFiltroDTO;
import com.masterserv.productos.entity.Auditoria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuditoriaSpecification {

    public Specification<Auditoria> getByFilters(AuditoriaFiltroDTO filtro) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Usuario (Coincidencia parcial e ignorando mayúsculas)
            if (filtro.getUsuario() != null && !filtro.getUsuario().isBlank()) {
                String term = "%" + filtro.getUsuario().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("usuario")), term));
            }

            // 2. Acción (Exacta)
            if (filtro.getAccion() != null && !filtro.getAccion().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("accion"), filtro.getAccion()));
            }

            // 3. Entidad (Exacta)
            if (filtro.getEntidad() != null && !filtro.getEntidad().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("entidad"), filtro.getEntidad()));
            }

            // 4. Fechas (Convirtiendo LocalDate a LocalDateTime para cubrir todo el día)
            if (filtro.getFechaDesde() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), filtro.getFechaDesde().atStartOfDay()));
            }
            if (filtro.getFechaHasta() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), filtro.getFechaHasta().atTime(23, 59, 59)));
            }

            // Ordenar por fecha descendente (lo más nuevo primero)
            query.orderBy(criteriaBuilder.desc(root.get("fecha")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}