package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ReglaPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Long> {

    /**
     * Busca una regla activa para la fecha actual.
     * Esto es clave para que el servicio sepa qué regla aplicar.
     */
    @Query("SELECT r FROM ReglaPuntos r WHERE (r.vigenciaDesde <= :fechaActual OR r.vigenciaDesde IS NULL) " +
           "AND (r.vigenciaHasta >= :fechaActual OR r.vigenciaHasta IS NULL)")
    Optional<ReglaPuntos> findReglaActiva(LocalDate fechaActual);
}