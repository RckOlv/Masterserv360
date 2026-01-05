package com.masterserv.productos.repository;

import com.masterserv.productos.entity.MovimientoPuntos;
import com.masterserv.productos.entity.Venta;
import com.masterserv.productos.enums.TipoMovimientoPuntos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Long> {
    
    // Encuentra el movimiento original de "GANADO" para una venta
    Optional<MovimientoPuntos> findByVentaAndTipoMovimiento(Venta venta, TipoMovimientoPuntos tipo);
    
    // Verifica si ya existe una reversión o expiración para esta venta
    boolean existsByVentaAndTipoMovimiento(Venta venta, TipoMovimientoPuntos tipo);

    // Busca movimientos de cierto tipo (GANADO) que hayan vencido antes de una fecha dada
    List<MovimientoPuntos> findByFechaCaducidadPuntosBeforeAndTipoMovimiento(LocalDateTime fecha, TipoMovimientoPuntos tipo);
}