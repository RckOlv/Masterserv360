package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    
    // Para la campanita: Dame las que NO están leídas, primero las más nuevas
    List<Alerta> findByLeidaFalseOrderByFechaCreacionDesc();

    // Para el historial: Dame todas ordenadas
    List<Alerta> findAllByOrderByFechaCreacionDesc();
}