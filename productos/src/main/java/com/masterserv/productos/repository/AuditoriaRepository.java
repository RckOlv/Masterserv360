package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    // Para mostrar los últimos movimientos primero
    List<Auditoria> findAllByOrderByFechaDesc();
    Page<Auditoria> findAllByOrderByFechaDesc(Pageable pageable);
}