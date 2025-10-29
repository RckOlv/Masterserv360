package com.masterserv.productos.repository;

import com.masterserv.productos.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// Podrías necesitar importar List si añades métodos que devuelvan listas
// import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    // Aquí podrían ir consultas específicas para detalles si fueran necesarias en el futuro.
    // Ej: List<DetalleVenta> findByProductoId(Long productoId);
    // Ej: Consultas para reportes de productos más vendidos, etc.
}