package com.masterserv.productos.specification;

import com.masterserv.productos.dto.UsuarioFiltroDTO;
import com.masterserv.productos.entity.Rol; // Importar
import com.masterserv.productos.entity.Usuario;
import jakarta.persistence.criteria.Join; // Importar
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UsuarioSpecification {

    public Specification<Usuario> getUsuariosByFilters(UsuarioFiltroDTO filtro) {
        return (root, query, cb) -> {
            
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por Nombre, Apellido, Email o Documento
            if (filtro.getNombreOEmail() != null && !filtro.getNombreOEmail().isEmpty()) {
                String busquedaLower = "%" + filtro.getNombreOEmail().toLowerCase() + "%";
                String busquedaNormal = "%" + filtro.getNombreOEmail() + "%"; // Para el documento (que no es lowercase)

                // --- ¡INICIO DE LA CORRECCIÓN! ---
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), busquedaLower),
                    cb.like(cb.lower(root.get("apellido")), busquedaLower),
                    cb.like(cb.lower(root.get("email")), busquedaLower),
                    cb.like(root.get("documento"), busquedaNormal) // ¡AÑADIDO! Busca también por documento
                ));
                // --- FIN DE LA CORRECCIÓN! ---
            }

            // Filtro por Documento (Este es un filtro separado, lo dejamos por si acaso)
            if (filtro.getDocumento() != null && !filtro.getDocumento().isEmpty()) {
                predicates.add(cb.like(root.get("documento"), "%" + filtro.getDocumento() + "%"));
            }

            // Filtro por Estado
            if (filtro.getEstado() != null) {
                predicates.add(cb.equal(root.get("estado"), filtro.getEstado()));
            }

            // Filtro por Rol ID (requiere un JOIN)
            if (filtro.getRolId() != null) {
                Join<Usuario, Rol> rolJoin = root.join("roles");
                predicates.add(cb.equal(rolJoin.get("id"), filtro.getRolId()));
            }
            
            // Evita que la consulta se duplique por el Join de roles
            query.distinct(true); 

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}