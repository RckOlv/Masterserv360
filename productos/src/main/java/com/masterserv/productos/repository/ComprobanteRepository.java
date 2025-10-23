package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Comprobante;
import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    // Métodos para encontrar un comprobante por su origen (relación 1:1)
    Optional<Comprobante> findByVenta(Venta venta);
    
    Optional<Comprobante> findByPedido(Pedido pedido);

    // Método para buscar por el número legal
    Optional<Comprobante> findByNumero(String numero);
}