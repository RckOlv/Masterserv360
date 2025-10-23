package com.masterserv.productos.repository;

import com.masterserv.productos.entity.CuentaPuntos;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CuentaPuntosRepository extends JpaRepository<CuentaPuntos, Long> {

    // Método para encontrar la "billetera" 1:1 de un cliente
    Optional<CuentaPuntos> findByCliente(Usuario cliente);
    
    Optional<CuentaPuntos> findByCliente_Id(Long clienteId);
}