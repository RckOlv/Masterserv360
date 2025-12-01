package com.masterserv.productos.repository;

import com.masterserv.productos.entity.SolicitudProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudProductoRepository extends JpaRepository<SolicitudProducto, Long> {
    // Aquí podrías agregar métodos como findByProcesadoFalse() para tu Dashboard
}