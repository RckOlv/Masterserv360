package com.masterserv.productos.specification;

import com.masterserv.productos.dto.PedidoFiltroDTO;
import com.masterserv.productos.entity.Pedido;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PedidoSpecification {

    public Specification<Pedido> getByFilters(PedidoFiltroDTO filtro) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro por Proveedor
            if (filtro.getProveedorId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("proveedor").get("id"), filtro.getProveedorId()));
            }

            // 2. Filtro por Usuario (Solicitante)
            if (filtro.getUsuarioId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("usuario").get("id"), filtro.getUsuarioId()));
            }

            // 3. Filtro por Estado
            if (filtro.getEstado() != null) {
                predicates.add(criteriaBuilder.equal(root.get("estado"), filtro.getEstado()));
            }

            // 4. Rango de Fechas (Fecha Pedido)
            if (filtro.getFechaDesde() != null) {
                // Mayor o igual al inicio del día (00:00:00)
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaPedido"), filtro.getFechaDesde().atStartOfDay()));
            }
            if (filtro.getFechaHasta() != null) {
                // Menor o igual al final del día (23:59:59)
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaPedido"), filtro.getFechaHasta().atTime(23, 59, 59)));
            }
            
            // Ordenar por defecto: Más recientes primero
            query.orderBy(criteriaBuilder.desc(root.get("fechaPedido")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}