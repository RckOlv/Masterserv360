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
     * Crea una especificación para buscar productos que coincidan con los filtros.
     * @param filtro El DTO con los criterios de búsqueda.
     * @return Una especificación de JPA.
     */
    public Specification<Producto> getProductosByFilters(ProductoFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // --- LÓGICA DE ESTADO CORREGIDA ---
            if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
                // Si el filtro es "TODOS", no añadimos ningún predicado de estado.
                if (!filtro.getEstado().equalsIgnoreCase("TODOS")) {
                    // Si es "INACTIVO" o "ACTIVO", aplicamos ese filtro.
                    predicates.add(cb.equal(root.get("estado"), filtro.getEstado())); 
                }
            } else {
                // Si no se especifica un estado (es null o ""), aplicamos el filtro por defecto "ACTIVO".
                predicates.add(cb.equal(root.get("estado"), "ACTIVO")); 
            }
            // ---------------------------------------------

            // --- FILTROS OPCIONALES DEL DTO ---

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
     * (Esta lógica ahora está incluida en el 'if' de arriba, pero la dejamos por si la necesitas
     * para un endpoint de admin que *nunca* filtre por estado).
     */
    public Specification<Producto> getAllProductosByFilters(ProductoFiltroDTO filtro) {
         return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- ESTA VERSIÓN NO INCLUYE NINGÚN FILTRO POR ESTADO ---

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

            // ... (etc. todos los demás filtros) ...
            
            return cb.and(predicates.toArray(new Predicate[0]));
         };
    }
}