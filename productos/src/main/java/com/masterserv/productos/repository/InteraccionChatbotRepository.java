package com.masterserv.productos.repository;

import com.masterserv.productos.entity.InteraccionChatbot;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteraccionChatbotRepository extends JpaRepository<InteraccionChatbot, Long> {

    // Método para buscar el historial de chat de un usuario específico
    List<InteraccionChatbot> findByUsuarioOrderByFechaDesc(Usuario usuario);
}