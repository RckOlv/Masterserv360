package com.masterserv.productos.repository;

import com.masterserv.productos.dto.TopProductoDTO;
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

    /**
     * Sobrescribe el findAll de JpaRepository para optimizarlo.
     * Carga el cliente y el vendedor asociados a cada venta en la
     * misma consulta para evitar N+1 en el listado.
     * NO cargamos los 'detalles' aquí, sería ineficiente para un listado.
     */
    @Override
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findAll(Pageable pageable);

    // --- Métodos de Búsqueda Adicionales (Ejemplos para el futuro) ---

    /**
     * Busca ventas para un cliente específico, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Busca ventas realizadas por un vendedor específico, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByVendedorId(Long vendedorId, Pageable pageable);

    /**
     * Busca ventas dentro de un rango de fechas, paginado.
     * Carga cliente y vendedor eficientemente.
     */
    @EntityGraph(attributePaths = {"cliente", "vendedor"})
    Page<Venta> findByFechaVentaBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);

    @Query("SELECT v FROM Venta v WHERE v.id = :id") // <-- AÑADIR ESTA LÍNEA
    @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto","cupon"})
    Optional<Venta> findByIdWithDetails(@Param("id") Long id); // <-- AÑADIR @Param

    //(Suma los totales de ventas COMPLETADAS de este mes)
    @Query("SELECT SUM(v.totalVenta) FROM Venta v WHERE v.estado = 'COMPLETADA' AND EXTRACT(MONTH FROM v.fechaVenta) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.fechaVenta) = EXTRACT(YEAR FROM CURRENT_DATE)")
    BigDecimal sumTotalVentasMesActual();

    Page<Venta> findByCliente(Usuario cliente, Pageable pageable);

    // Consulta para el gráfico de líneas: Suma las ventas por día
    @Query("SELECT new com.masterserv.productos.dto.VentasPorDiaDTO(CAST(v.fechaVenta AS LocalDate), SUM(v.totalVenta)) " +
           "FROM Venta v " +
           "WHERE v.estado = 'COMPLETADA' AND v.fechaVenta >= :fechaInicio " +
           "GROUP BY CAST(v.fechaVenta AS LocalDate) " +
           "ORDER BY CAST(v.fechaVenta AS LocalDate) ASC")
    List<VentasPorDiaDTO> findVentasSumarizadasPorDia(@Param("fechaInicio") LocalDateTime fechaInicio);

    // Consulta para el gráfico de barras: Cuenta los productos más vendidos
    @Query("SELECT new com.masterserv.productos.dto.TopProductoDTO(dv.producto.id, dv.producto.nombre, SUM(dv.cantidad)) " +
           "FROM DetalleVenta dv " +
           "WHERE dv.venta.estado = 'COMPLETADA' AND dv.venta.fechaVenta >= :fechaInicio " +
           "GROUP BY dv.producto.id, dv.producto.nombre " +
           "ORDER BY SUM(dv.cantidad) DESC " +
           "LIMIT 5")
    List<TopProductoDTO> findTop5ProductosVendidos(@Param("fechaInicio") LocalDateTime fechaInicio);

    @Query("SELECT SUM(v.totalVenta) FROM Venta v WHERE v.estado = 'COMPLETADA' AND v.fechaVenta BETWEEN :inicio AND :fin")
    Optional<BigDecimal> findTotalVentasEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}