package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca un Pedido por ID y carga EAGERLY sus detalles 
     * y los productos de esos detalles.
     */
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Pedido> findByIdWithDetails(Long id);
}