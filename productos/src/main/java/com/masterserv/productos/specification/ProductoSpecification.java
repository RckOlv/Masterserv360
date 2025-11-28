package com.masterserv.productos.specification;

import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductoSpecification {

    /**
     * Crea una especificación para el ADMIN/VENDEDOR.
     */
    public Specification<Producto> getProductosByFilters(ProductoFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // 1. Estado (ACTIVO / INACTIVO / TODOS)
            if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
                if (!filtro.getEstado().equalsIgnoreCase("TODOS")) {
                    predicates.add(cb.equal(root.get("estado"), filtro.getEstado())); 
                }
            } else {
                // Por defecto solo activos si no se especifica
                predicates.add(cb.equal(root.get("estado"), "ACTIVO")); 
            }

            // 2. Nombre
            if (filtro.getNombre() != null && !filtro.getNombre().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().toLowerCase() + "%"));
            }

            // 3. Código
            if (filtro.getCodigo() != null && !filtro.getCodigo().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codigo")), "%" + filtro.getCodigo().toLowerCase() + "%"));
            }

            // 4. Categoría ID
            if (filtro.getCategoriaId() != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), filtro.getCategoriaId()));
            }
            
            // 5. Precio Máximo
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            // 6. --- FILTRO DE STOCK UNIFICADO ---
            if (filtro.getEstadoStock() != null) {
                switch (filtro.getEstadoStock()) {
                    case "CON_STOCK":
                        predicates.add(cb.greaterThan(root.get("stockActual"), 0));
                        break;
                    case "SIN_STOCK":
                        predicates.add(cb.equal(root.get("stockActual"), 0));
                        break;
                    case "STOCK_BAJO":
                        // stockActual <= stockMinimo
                        predicates.add(cb.lessThanOrEqualTo(root.get("stockActual"), root.get("stockMinimo")));
                        break;
                    default: // "TODOS"
                        break;
                }
            }
            // Compatibilidad vieja (si el front mandara el booleano)
            else if (filtro.getConStock() != null) {
                if (filtro.getConStock()) {
                    predicates.add(cb.greaterThan(root.get("stockActual"), 0));
                } else {
                    predicates.add(cb.equal(root.get("stockActual"), 0));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    

    /**
     * Crea una especificación para el CATÁLOGO PÚBLICO.
     */
    public Specification<Producto> getPublicProductosByFilters(ProductoPublicoFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // Siempre ACTIVO
            predicates.add(cb.equal(root.get("estado"), "ACTIVO"));

            if (filtro.getNombre() != null && !filtro.getNombre().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().toLowerCase() + "%"));
            }

            if (!CollectionUtils.isEmpty(filtro.getCategoriaIds())) {
                predicates.add(root.get("categoria").get("id").in(filtro.getCategoriaIds()));
            }

            if (filtro.getPrecioMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMin()));
            }
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            if (filtro.getSoloConStock() != null && filtro.getSoloConStock()) {
                predicates.add(cb.greaterThan(root.get("stockActual"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}