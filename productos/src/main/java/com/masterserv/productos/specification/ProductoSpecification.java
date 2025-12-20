package com.masterserv.productos.specification;

import com.masterserv.productos.dto.ProductoFiltroDTO;
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
     * Crea una especificación UNIFICADA para ADMIN y CATÁLOGO PÚBLICO.
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

            // 4. Categoría (Soporta ID único o Lista de IDs)
            if (filtro.getCategoriaId() != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), filtro.getCategoriaId()));
            }
            // NUEVO: Soporte para lista de categorías (del catálogo público)
            if (!CollectionUtils.isEmpty(filtro.getCategoriaIds())) {
                 predicates.add(root.get("categoria").get("id").in(filtro.getCategoriaIds()));
            }
            
            // 5. Rango de Precios
            if (filtro.getPrecioMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMin()));
            }
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            // 6. Stock Unificado
            // Lógica del Admin (estadoStock)
            if (filtro.getEstadoStock() != null) {
                switch (filtro.getEstadoStock()) {
                    case "CON_STOCK":
                        predicates.add(cb.greaterThan(root.get("stockActual"), 0));
                        break;
                    case "SIN_STOCK":
                        predicates.add(cb.equal(root.get("stockActual"), 0));
                        break;
                    case "STOCK_BAJO":
                        predicates.add(cb.lessThanOrEqualTo(root.get("stockActual"), root.get("stockMinimo")));
                        break;
                    default: break;
                }
            }
            // Lógica del Catálogo Público (soloConStock) o Admin viejo (conStock)
            else if (Boolean.TRUE.equals(filtro.getSoloConStock()) || Boolean.TRUE.equals(filtro.getConStock())) {
                predicates.add(cb.greaterThan(root.get("stockActual"), 0));
            } else if (Boolean.FALSE.equals(filtro.getConStock())) { // Solo si explícitamente pide sin stock
                 predicates.add(cb.equal(root.get("stockActual"), 0));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    // El método getPublicProductosByFilters ya no es necesario si usas el de arriba, 
    // pero puedes dejarlo o borrarlo. Lo importante es que el Controller use el de arriba.
}