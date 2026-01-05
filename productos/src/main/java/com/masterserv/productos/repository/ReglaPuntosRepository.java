package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ReglaPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- Mentor: Importar
import org.springframework.data.repository.query.Param; // <-- Mentor: Importar
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Long> {
    // Spring Boot generará automáticamente el SQL para buscar por el campo 'estadoRegla'.
    Optional<ReglaPuntos> findByEstadoRegla(@Param("estado") String estadoRegla);
}