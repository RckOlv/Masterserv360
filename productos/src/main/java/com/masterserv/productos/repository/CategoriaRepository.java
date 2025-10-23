package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    // Método para buscar por nombre, útil para evitar duplicados
    Optional<Categoria> findByNombre(String nombre);
}