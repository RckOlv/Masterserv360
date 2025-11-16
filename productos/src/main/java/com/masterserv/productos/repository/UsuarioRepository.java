package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Importado
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
// 1. AÑADIDO JpaSpecificationExecutor
public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    /**
     * Busca un usuario por su email.
     * ¡@EntityGraph es fundamental para que el Login cargue los roles!
     */
    @EntityGraph(attributePaths = "roles") 
    Optional<Usuario> findByEmail(String email);

    // Método para verificar si un email ya existe (útil para el registro)
    boolean existsByEmail(String email);

    // Método para verificar si un documento ya existe
    boolean existsByDocumentoAndTipoDocumento_Id(String documento, Long tipoDocumentoId);
    
    // Método para el Chatbot
    Optional<Usuario> findByTelefono(String telefono);
    
    /**
     * Sobreescribimos findAll (de JpaSpecificationExecutor) para forzar
     * la carga EAGER de los roles y tipoDocumento (Soluciona N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"roles", "tipoDocumento"})
    Page<Usuario> findAll(Specification<Usuario> spec, Pageable pageable);

    //(Cuenta clientes que están ACTIVOS)
    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.nombreRol = 'ROLE_CLIENTE' AND u.estado = 'ACTIVO'")
    long countClientesActivos();

    Optional<Usuario> findByDocumento(String documento);
    
    @Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.nombreRol = 'ROLE_ADMIN' AND u.estado = 'ACTIVO'")
    long countActiveAdmins();
}