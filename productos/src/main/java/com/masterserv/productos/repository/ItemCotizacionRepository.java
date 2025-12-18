package com.masterserv.productos.repository;

import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemCotizacionRepository extends JpaRepository<ItemCotizacion, Long> {

    /**
     * Busca items "rivales" (mismo producto) en otras cotizaciones que todavía están vivas (PENDIENTE o COTIZADO),
     * excluyendo la cotización que acaba de ganar.
     */
    @Query("SELECT i FROM ItemCotizacion i " +
           "WHERE i.producto.id = :productoId " +
           "AND i.estado IN (:estadosVivos) " +
           "AND i.cotizacion.id <> :idCotizacionGanadora")
    List<ItemCotizacion> findItemsRivales(
            @Param("productoId") Long productoId,
            @Param("idCotizacionGanadora") Long idCotizacionGanadora,
            @Param("estadosVivos") List<EstadoItemCotizacion> estadosVivos
    );
}