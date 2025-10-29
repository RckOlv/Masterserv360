package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.entity.Usuario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Importa @Param
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Busca un Pedido por ID y carga EAGERLY sus detalles 
     * y los productos de esos detalles.
     * * AÑADIMOS @Query para que Spring Data JPA no intente parsear
     * el nombre "findByIdWithDetails" y use esta consulta en su lugar.
     * La anotación @EntityGraph se aplica sobre esta consulta.
     */
    @Query("SELECT p FROM Pedido p WHERE p.id = :id") // <-- ESTA LÍNEA ES LA NUEVA
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Pedido> findByIdWithDetails(@Param("id") Long id); // <-- Añadido @Param por buena práctica

    @Override
    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<Pedido> findAll(Pageable pageable);

}