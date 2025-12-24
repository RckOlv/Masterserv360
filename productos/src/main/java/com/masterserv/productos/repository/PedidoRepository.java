package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Pedido;
import com.masterserv.productos.enums.EstadoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Importante
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    @Query("SELECT p FROM Pedido p WHERE p.id = :id") 
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Pedido> findByIdWithDetails(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    Page<Pedido> findAll(Pageable pageable);

    Optional<Pedido> findByToken(String token);

    List<Pedido> findByEstado(EstadoPedido estado);

    // --- NUEVO MÉTODO PARA ALERTAS ---
    // Busca pedidos "EN_CAMINO" que tengan una fecha estimada X
    List<Pedido> findByEstadoAndFechaEntregaEstimada(EstadoPedido estado, LocalDate fecha);
}