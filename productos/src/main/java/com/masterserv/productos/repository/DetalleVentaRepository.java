package com.masterserv.productos.repository;

import com.masterserv.productos.entity.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    
    // Al igual que DetallePedido, este repositorio 
    // rara vez necesita métodos custom.
}