package com.masterserv.productos.repository;

import com.masterserv.productos.entity.CuentaPuntos;
import com.masterserv.productos.entity.MovimientoPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Long> {

    /**
     * Obtiene el "extracto bancario" de una cuenta de puntos.
     * Fundamental para mostrar el historial al cliente.
     */
    List<MovimientoPuntos> findByCuentaPuntosOrderByFechaDesc(CuentaPuntos cuentaPuntos);
}