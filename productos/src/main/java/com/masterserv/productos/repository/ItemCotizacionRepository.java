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

    @Query("SELECT i FROM ItemCotizacion i " +
           "WHERE i.producto.id = :productoId " +
           "AND i.id <> :itemGanadorId " +
           "AND i.estado IN (:estadosVivos)")
    List<ItemCotizacion> findItemsRivales(
            @Param("productoId") Long productoId,
            @Param("itemGanadorId") Long itemGanadorId,
            @Param("estadosVivos") List<EstadoItemCotizacion> estadosVivos
    );

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ItemCotizacion i " +
           "WHERE i.producto = :producto " +
           "AND i.cotizacion.proveedor = :proveedor " +
           "AND i.cotizacion.estado IN :estados")
    boolean existePedidoActivo(@Param("producto") Producto producto, 
                               @Param("proveedor") Proveedor proveedor, 
                               @Param("estados") Collection<EstadoCotizacion> estados);

    // 1. Obtener lista de productos listos para comparar (Solo los que ya tienen precio: COTIZADO)
    @Query("SELECT i.producto.id as productoId, " +
           "i.producto.nombre as nombre, " +
           "i.producto.codigo as codigo, " +
           "i.producto.imagenUrl as imagenUrl, " +
           "COUNT(distinct i.cotizacion.id) as cantidadCotizaciones, " +
           "MIN(i.precioUnitarioOfertado) as mejorPrecio " +
           "FROM ItemCotizacion i " +
           "WHERE i.cotizacion.estado = 'RECIBIDA' " +
           "AND i.estado = com.masterserv.productos.enums.EstadoItemCotizacion.COTIZADO " + // 👈 CAMBIADO A COTIZADO
           "GROUP BY i.producto.id, i.producto.nombre, i.producto.codigo, i.producto.imagenUrl")
    List<ResumenProductoCompraDTO> findProductosEnCotizacionesRecibidas();

    // 2. Obtener el detalle de quién cotizó ese producto específico
    @Query("SELECT new com.masterserv.productos.dto.DetalleComparativaDTO(" +
           "i.id, " + 
           "c.id, " +
           "p.razonSocial, " +
           "i.precioUnitarioOfertado, " +
           "i.cantidadSolicitada, " +
           "c.fechaEntregaOfertada, " +
           "c.esRecomendada) " +
           "FROM ItemCotizacion i " +
           "JOIN i.cotizacion c " +
           "JOIN c.proveedor p " +
           "WHERE i.producto.id = :productoId AND c.estado = 'RECIBIDA' " +
           "AND i.estado = com.masterserv.productos.enums.EstadoItemCotizacion.COTIZADO " + 
           "ORDER BY i.precioUnitarioOfertado ASC")
    List<DetalleComparativaDTO> findComparativaPorProducto(@Param("productoId") Long productoId);

    List<ItemCotizacion> findByProductoIdAndEstado(Long productoId, com.masterserv.productos.enums.EstadoItemCotizacion estado);
}