package com.masterserv.productos.repository;

import com.masterserv.productos.entity.PasswordResetToken;
import com.masterserv.productos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUsuario(Usuario usuario); // Para borrar tokens viejos si pide uno nuevo
}