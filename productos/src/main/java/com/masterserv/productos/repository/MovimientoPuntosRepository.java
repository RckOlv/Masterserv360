package com.masterserv.productos.repository;

import com.masterserv.productos.entity.MovimientoPuntos;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.enums.TipoMovimientoPuntos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Long> {
    
    // Encuentra el movimiento original de "GANADO" para una venta
    Optional<MovimientoPuntos> findByVentaAndTipoMovimiento(Venta venta, TipoMovimientoPuntos tipo);
    
    // Verifica si ya existe una reversión para esta venta
    boolean existsByVentaAndTipoMovimiento(Venta venta, TipoMovimientoPuntos tipo);
}