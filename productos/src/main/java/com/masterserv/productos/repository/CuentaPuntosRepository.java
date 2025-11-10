package com.masterserv.productos.repository;

import com.masterserv.productos.entity.CuentaPuntos;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaPuntosRepository extends JpaRepository<CuentaPuntos, Long> {

    /**
     * Busca la cuenta de puntos asociada a un cliente/usuario específico.
     * Es clave para la integración con el módulo de Ventas.
     */
    Optional<CuentaPuntos> findByCliente(Usuario cliente);

    /**
     * Busca por el ID del cliente.
     */
    Optional<CuentaPuntos> findByClienteId(Long clienteId);

    // Nos permite encontrar la cuenta directamente desde el email del Principal (Spring Security)
    Optional<CuentaPuntos> findByCliente_Email(String clienteEmail);
}