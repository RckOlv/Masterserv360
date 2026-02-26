package com.masterserv.productos.repository; // Ajusta tu paquete

import com.masterserv.productos.entity.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CajaRepository extends JpaRepository<Caja, Long> {
    
    // Busca si el usuario ya tiene una caja abierta
    @Query("SELECT c FROM Caja c WHERE c.usuario.id = :usuarioId AND c.estado = 'ABIERTA'")
    Optional<Caja> findCajaAbiertaByUsuario(@Param("usuarioId") Long usuarioId);

    Optional<Caja> findFirstByEstado(String estado);
}