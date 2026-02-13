package com.masterserv.productos.repository;

import com.masterserv.productos.dto.DetalleComparativaDTO;
import com.masterserv.productos.dto.ResumenProductoCompraDTO;
import com.masterserv.productos.entity.ItemCotizacion;
import com.masterserv.productos.entity.Producto;
import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.enums.EstadoCotizacion;
import com.masterserv.productos.enums.EstadoItemCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    // Busca si existe algún item de este producto, para este proveedor, 
    // dentro de una cotización que esté en estado "Pendiente", "Recibida" o "Confirmada".
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ItemCotizacion i " +
           "WHERE i.producto = :producto " +
           "AND i.cotizacion.proveedor = :proveedor " +
           "AND i.cotizacion.estado IN :estados")
    boolean existePedidoActivo(@Param("producto") Producto producto, 
                               @Param("proveedor") Proveedor proveedor, 
                               @Param("estados") Collection<EstadoCotizacion> estados);


    // 1. Obtener lista de productos que están en cotizaciones RECIBIDAS (listos para comparar)
    @Query("SELECT i.producto.id as productoId, " +
           "i.producto.nombre as nombre, " +
           "i.producto.codigo as codigo, " +
           "i.producto.imagenUrl as imagenUrl, " +
           "COUNT(distinct i.cotizacion.id) as cantidadCotizaciones, " +
           "MIN(i.precioUnitarioOfertado) as mejorPrecio " +
           "FROM ItemCotizacion i " +
           "WHERE i.cotizacion.estado = 'RECIBIDA' " +
           "GROUP BY i.producto.id, i.producto.nombre, i.producto.codigo, i.producto.imagenUrl")
    List<ResumenProductoCompraDTO> findProductosEnCotizacionesRecibidas();

    // 2. Obtener el detalle de quién cotizó ese producto específico
    @Query("SELECT new com.masterserv.productos.dto.DetalleComparativaDTO(" +
           "c.id, p.razonSocial, i.precioUnitarioOfertado, i.cantidadSolicitada, c.fechaEntregaOfertada, c.esRecomendada) " +
           "FROM ItemCotizacion i " +
           "JOIN i.cotizacion c " +
           "JOIN c.proveedor p " +
           "WHERE i.producto.id = :productoId AND c.estado = 'RECIBIDA' " +
           "ORDER BY i.precioUnitarioOfertado ASC")
    List<DetalleComparativaDTO> findComparativaPorProducto(@Param("productoId") Long productoId);
}