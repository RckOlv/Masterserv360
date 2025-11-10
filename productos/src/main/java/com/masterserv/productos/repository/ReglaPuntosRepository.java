package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ReglaPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Long> {

    /**
     * Busca la regla de puntos que está ACTIVA.
     * Asumimos que solo una regla debe estar en estado 'ACTIVA'.
     */
    Optional<ReglaPuntos> findByEstadoRegla(String estadoRegla); // Usaremos findByEstadoRegla("ACTIVA")
}