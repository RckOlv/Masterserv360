package com.masterserv.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.masterserv.productos.entity.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {}