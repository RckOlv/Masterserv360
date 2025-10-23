package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email.
     * Este método es fundamental para el proceso de Login (UserDetailsService).
     * Usamos @EntityGraph para cargar los roles EAGERLY en esta consulta específica.
     */
    @Override
    @EntityGraph(attributePaths = "roles") // Carga los roles junto con el usuario
    Optional<Usuario> findById(Long id);

    /**
     * Busca un usuario por su email.
     * Este método es fundamental para el proceso de Login (UserDetailsService).
     * Usamos Optional para manejar de forma segura si el usuario no existe.
     * Spring Data JPA implementa esto automáticamente.
     */
    Optional<Usuario> findByEmail(String email);

    // Método para verificar si un email ya existe (útil para el registro)
    boolean existsByEmail(String email);

    // Método para verificar si un documento ya existe
    boolean existsByDocumentoAndTipoDocumento_Id(String documento, Long tipoDocumentoId);

	Optional<Usuario> findByTelefono(String telefono);
    
}