package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Cupon;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.enums.EstadoCupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuponRepository extends JpaRepository<Cupon, Long> {

    /**
     * Busca un cupón por su código.
     * Esencial para validarlo en una venta.
     */
    Optional<Cupon> findByCodigo(String codigo);

    /**
     * Busca todos los cupones de un cliente específico.
     * Útil para que el cliente vea sus cupones disponibles.
     */
    List<Cupon> findByCliente(Usuario cliente);

    /**
     * Busca cupones de un cliente por estado (ej. "VIGENTE").
     */
    List<Cupon> findByClienteAndEstado(Usuario cliente, EstadoCupon estado);
}