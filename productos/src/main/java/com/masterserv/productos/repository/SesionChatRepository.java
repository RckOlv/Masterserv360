package com.masterserv.productos.repository;

import com.masterserv.productos.entity.SesionChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SesionChatRepository extends JpaRepository<SesionChat, Long> {
    Optional<SesionChat> findByTelefono(String telefono);
}