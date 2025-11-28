package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ReglaPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- Mentor: Importar
import org.springframework.data.repository.query.Param; // <-- Mentor: Importar
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Long> {

    /**
     * Busca la regla de puntos que está ACTIVA.
     * * MENTOR - MODIFICACIÓN:
     * Usamos @Query con "LEFT JOIN FETCH" para obligar a Hibernate a 
     * traer la lista de 'recompensas' en la misma consulta SQL.
     * Esto soluciona el problema de que no aparezcan en el frontend.
     */
    @Query("SELECT r FROM ReglaPuntos r LEFT JOIN FETCH r.recompensas WHERE r.estadoRegla = :estado")
    Optional<ReglaPuntos> findByEstadoRegla(@Param("estado") String estadoRegla);
}