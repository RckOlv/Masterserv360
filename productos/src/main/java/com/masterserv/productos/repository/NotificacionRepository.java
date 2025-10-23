package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Notificacion;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Busca todas las notificaciones enviadas a un usuario específico.
     * Útil para un "centro de notificaciones" en el frontend.
     */
    List<Notificacion> findByUsuarioOrderByFechaEnvioDesc(Usuario usuario);
}