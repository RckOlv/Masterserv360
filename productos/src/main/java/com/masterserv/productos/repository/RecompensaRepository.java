package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Recompensa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecompensaRepository extends JpaRepository<Recompensa, Long> {
	
	// CORRECCIÓN: Cambiamos 'findByNombre...' por 'findByDescripcion...'
    // porque tu entidad usa el campo 'descripcion'.
    List<Recompensa> findByDescripcionContainingIgnoreCase(String descripcion);
}