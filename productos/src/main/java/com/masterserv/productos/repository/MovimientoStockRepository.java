package com.masterserv.productos.repository;

import com.masterserv.productos.entity.MovimientoStock;
import com.masterserv.productos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    // Método para obtener el historial de un producto específico
    List<MovimientoStock> findByProductoOrderByFechaDesc(Producto producto);
}