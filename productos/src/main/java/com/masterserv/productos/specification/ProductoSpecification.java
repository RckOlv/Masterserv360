package com.masterserv.productos.specification;

import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.entity.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductoSpecification {

    /**
     * Crea una especificación para buscar productos ACTIVOS que coincidan con los filtros.
     * @param filtro El DTO con los criterios de búsqueda (nombre, código, categoría, etc.).
     * @return Una especificación de JPA para usar en el repositorio.
     */
    public Specification<Producto> getProductosByFilters(ProductoFiltroDTO filtro) {
        // (root, query, criteriaBuilder)
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // --- PREDICADO OBLIGATORIO: SOLO ACTIVOS ---
            // Por defecto, siempre filtramos para mostrar solo productos con estado "ACTIVO"
            predicates.add(cb.equal(root.get("estado"), "ACTIVO")); 
            // ---------------------------------------------

            // --- FILTROS OPCIONALES DEL DTO ---

            // Filtro por NOMBRE (usando LIKE, ignorando mayúsculas/minúsculas)
            if (filtro.getNombre() != null && !filtro.getNombre().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().toLowerCase() + "%"));
            }

            // Filtro por CODIGO (usando LIKE, ignorando mayúsculas/minúsculas)
            if (filtro.getCodigo() != null && !filtro.getCodigo().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codigo")), "%" + filtro.getCodigo().toLowerCase() + "%"));
            }

            // Filtro por CATEGORIA ID (igualdad exacta)
            if (filtro.getCategoriaId() != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), filtro.getCategoriaId()));
            }

            // Filtro por PRECIO MÁXIMO (menor o igual a)
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            // Filtro por STOCK (si tiene stock o no)
            if (filtro.getConStock() != null) {
                if (filtro.getConStock()) {
                    predicates.add(cb.greaterThan(root.get("stockActual"), 0)); // Con stock ( > 0 )
                } else {
                    predicates.add(cb.equal(root.get("stockActual"), 0)); // Sin stock ( = 0 )
                }
            }

            // Combina todos los predicados (filtros) con un AND lógico
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Opcional: Crea una especificación para buscar TODOS los productos (incluyendo inactivos)
     * que coincidan con los filtros. Útil para vistas de administrador.
     * @param filtro El DTO con los criterios de búsqueda.
     * @return Una especificación de JPA.
     */
    public Specification<Producto> getAllProductosByFilters(ProductoFiltroDTO filtro) {
         return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- ESTA VERSIÓN NO INCLUYE EL FILTRO POR ESTADO = 'ACTIVO' ---

            // Filtro por NOMBRE
            if (filtro.getNombre() != null && !filtro.getNombre().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().toLowerCase() + "%"));
            }

            // Filtro por CODIGO
            if (filtro.getCodigo() != null && !filtro.getCodigo().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("codigo")), "%" + filtro.getCodigo().toLowerCase() + "%"));
            }

            // Filtro por CATEGORIA ID
            if (filtro.getCategoriaId() != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), filtro.getCategoriaId()));
            }

            // Filtro por PRECIO MÁXIMO
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            // Filtro por STOCK
            if (filtro.getConStock() != null) {
                if (filtro.getConStock()) {
                    predicates.add(cb.greaterThan(root.get("stockActual"), 0)); 
                } else {
                    predicates.add(cb.equal(root.get("stockActual"), 0)); 
                }
            }

            // Combina los filtros
            return cb.and(predicates.toArray(new Predicate[0]));
         };
    }
}