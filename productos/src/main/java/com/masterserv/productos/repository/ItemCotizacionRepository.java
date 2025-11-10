package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ItemCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemCotizacionRepository extends JpaRepository<ItemCotizacion, Long> {
}