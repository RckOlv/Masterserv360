package com.masterserv.productos.repository;

import com.masterserv.productos.dto.TopProductoDTO;
import com.masterserv.productos.dto.VentasPorCategoriaDTO;
import com.masterserv.productos.dto.VentasPorDiaDTO;
import com.masterserv.productos.entity.Usuario;
import com.masterserv.productos.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByClienteId(Long clienteId, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByVendedorId(Long vendedorId, Pageable pageable);

    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByFechaVentaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto", "cupon"}) 
    Optional<Venta> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT SUM(v.totalVenta) FROM Venta v WHERE v.estado = 'COMPLETADA' AND EXTRACT(MONTH FROM v.fechaVenta) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.fechaVenta) = EXTRACT(YEAR FROM CURRENT_DATE)")
    BigDecimal sumTotalVentasMesActual();

    Page<Venta> findByCliente(Usuario cliente, Pageable pageable);

    // --- GRÁFICO DE LÍNEAS (EVOLUCIÓN) ---
    @Query("SELECT new com.masterserv.productos.dto.VentasPorDiaDTO(CAST(v.fechaVenta AS LocalDate), SUM(v.totalVenta)) " +
           "FROM Venta v " +
           "WHERE v.estado = 'COMPLETADA' AND v.fechaVenta BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY CAST(v.fechaVenta AS LocalDate) " +
           "ORDER BY CAST(v.fechaVenta AS LocalDate) ASC")
    List<VentasPorDiaDTO> findVentasSumarizadasPorDia(
            @Param("fechaInicio") LocalDateTime fechaInicio, 
            @Param("fechaFin") LocalDateTime fechaFin);

    // --- TOP PRODUCTOS (CORREGIDO) ---
    // 1. Agregado el parámetro fechaFin.
    // 2. Cambiada la condición a BETWEEN para respetar el filtro del dashboard.
    // 3. Eliminado 'LIMIT 5' para evitar errores de sintaxis JPQL (Hibernate maneja esto mejor si usas Pageable, pero así compilará ya mismo).
    @Query("SELECT new com.masterserv.productos.dto.TopProductoDTO(dv.producto.id, dv.producto.nombre, SUM(dv.cantidad)) " +
           "FROM DetalleVenta dv " +
           "WHERE dv.venta.estado = 'COMPLETADA' AND dv.venta.fechaVenta BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY dv.producto.id, dv.producto.nombre " +
           "ORDER BY SUM(dv.cantidad) DESC")
    List<TopProductoDTO> findTop5ProductosVendidos(
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    // --- TOTAL VENTAS ENTRE FECHAS ---
    @Query("SELECT SUM(v.totalVenta) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaVenta BETWEEN :inicio AND :fin")
    Optional<BigDecimal> findTotalVentasEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // --- CANTIDAD DE VENTAS EN RANGO ---
    @Query("SELECT COUNT(v) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaVenta BETWEEN :inicio AND :fin")
    long countVentasEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    // --- GRÁFICO DE DONA (CATEGORÍAS) ---
    @Query("SELECT new com.masterserv.productos.dto.VentasPorCategoriaDTO(p.categoria.nombre, SUM(dv.precioUnitario * dv.cantidad)) " +
           "FROM DetalleVenta dv " +
           "JOIN dv.producto p " +
           "JOIN dv.venta v " +
           "WHERE v.estado = 'COMPLETADA' AND v.fechaVenta BETWEEN :inicio AND :fin " +
           "GROUP BY p.categoria.nombre")
    List<VentasPorCategoriaDTO> findVentasPorCategoria(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}