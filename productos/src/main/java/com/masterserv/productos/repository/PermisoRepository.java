package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    // Spring Data JPA crea automáticamente los métodos CRUD
}