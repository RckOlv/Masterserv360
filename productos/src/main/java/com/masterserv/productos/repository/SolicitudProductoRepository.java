package com.masterserv.productos.repository;

import com.masterserv.productos.entity.SolicitudProducto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudProductoRepository extends JpaRepository<SolicitudProducto, Long> {
    List<SolicitudProducto> findByDescripcionContainingIgnoreCaseAndProcesadoFalse(String descripcion);
}