package com.masterserv.productos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.masterserv.productos.entity.Sesion;

public interface SesionRepository extends JpaRepository<Sesion, Long> {}