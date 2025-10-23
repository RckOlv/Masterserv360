package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Rol;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {
    
    // Necesitaremos un método para buscar un rol por su nombre
    // Spring Data JPA lo implementa solo por el nombre del método
    Optional<Rol> findByNombreRol(String nombreRol);
}