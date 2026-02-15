package com.masterserv.productos.repository;

import com.masterserv.productos.entity.EmpresaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaConfigRepository extends JpaRepository<EmpresaConfig, Long> {
    // No necesitamos métodos especiales, usaremos siempre el ID 1
}