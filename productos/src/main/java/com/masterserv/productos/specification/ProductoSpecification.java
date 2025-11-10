package com.masterserv.productos.specification;

import com.masterserv.productos.dto.ProductoFiltroDTO;
import com.masterserv.productos.dto.ProductoPublicoFiltroDTO; // <-- ¡IMPORTAR NUEVO DTO!
import com.masterserv.productos.entity.Producto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils; // <-- Para chequear listas vacías

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductoSpecification {

    /**
     * Crea una especificación para el ADMIN/VENDEDOR.
     * (Este es tu método original, está perfecto).
     */
    public Specification<Producto> getProductosByFilters(ProductoFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // --- LÓGICA DE ESTADO (PARA ADMIN) ---
            if (filtro.getEstado() != null && !filtro.getEstado().isEmpty()) {
                if (!filtro.getEstado().equalsIgnoreCase("TODOS")) {
                    predicates.add(cb.equal(root.get("estado"), filtro.getEstado())); 
                }
            } else {
                predicates.add(cb.equal(root.get("estado"), "ACTIVO")); 
            }
            // ---------------------------------------------

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
            
            // ... (Tus otros filtros de admin: precioMax, conStock, etc.) ...
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }
            if (filtro.getConStock() != null) {
                if (filtro.getConStock()) {
                    predicates.add(cb.greaterThan(root.get("stockActual"), 0));
                } else {
                    predicates.add(cb.equal(root.get("stockActual"), 0));
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    

    // --- ¡NUEVO MÉTODO PARA EL PORTAL DE CLIENTE! ---

    /**
     * Crea una especificación para el CATÁLOGO PÚBLICO (Cliente / Chatbot).
     * @param filtro El DTO público (ProductoPublicoFiltroDTO).
     * @return Una especificación de JPA.
     */
    public Specification<Producto> getPublicProductosByFilters(ProductoPublicoFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // --- ¡IMPORTANTE! FILTRO DE ESTADO FIJO ---
            // El público SOLO puede ver productos ACTIVOS. Este filtro no es opcional.
            predicates.add(cb.equal(root.get("estado"), "ACTIVO"));
            // -------------------------------------------

            // Filtro por NOMBRE (para la barra de búsqueda del cliente)
            if (filtro.getNombre() != null && !filtro.getNombre().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().toLowerCase() + "%"));
            }

            // Filtro por LISTA de CATEGORIAS (ej. el cliente tildó "Frenos" y "Filtros")
            if (!CollectionUtils.isEmpty(filtro.getCategoriaIds())) {
                // Crea una cláusula "IN": WHERE categoria.id IN (1, 2, 5)
                predicates.add(root.get("categoria").get("id").in(filtro.getCategoriaIds()));
            }

            // Filtro por RANGO DE PRECIO
            if (filtro.getPrecioMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMin()));
            }
            if (filtro.getPrecioMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("precioVenta"), filtro.getPrecioMax()));
            }

            // Filtro "Solo con Stock"
            if (filtro.getSoloConStock() != null && filtro.getSoloConStock()) {
                predicates.add(cb.greaterThan(root.get("stockActual"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    // (Tu método 'getAllProductosByFilters' está bien, lo puedes dejar o quitar si ya no lo usas)
}