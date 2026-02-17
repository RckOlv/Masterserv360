package com.masterserv.productos.repository;

import com.masterserv.productos.dto.reporte.VariacionCostoDTO;
import com.masterserv.productos.entity.DetallePedido;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {
    
    @Query("SELECT p.nombre AS producto, " +
           "prov.razonSocial AS proveedor, " +
           "CAST(ped.fechaCreacion AS date) AS fechaCompra, " +
           "dp.precioUnitario AS costoPagado, " + // Este es el costo pactado en el pedido
           "ped.nroPedido AS nroOrden " +
           "FROM DetallePedido dp " +
           "JOIN dp.producto p " +
           "JOIN dp.pedido ped " +
           "JOIN ped.proveedor prov " +
           "WHERE p.id = :productoId AND ped.estado = 'RECIBIDO' " +
           "ORDER BY ped.fechaCreacion DESC")
    List<VariacionCostoDTO> obtenerHistorialCostos(@Param("productoId") Long productoId);
}