package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Método para buscar por CUIT (clave única)
    Optional<Proveedor> findByCuit(String cuit);

    // Método para buscar por Razón Social
    Optional<Proveedor> findByRazonSocial(String razonSocial);
}