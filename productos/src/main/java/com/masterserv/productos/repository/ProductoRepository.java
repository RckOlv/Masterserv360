package com.masterserv.productos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.masterserv.productos.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoria_IdCategoria(Long idCategoria);

    List<Producto> findByNombreProductoContainingIgnoreCase(String nombre);

    Optional<Producto> findByNombreProducto(String nombreProducto);

    boolean existsByCodigo(String codigo);

    boolean existsByNombreProductoIgnoreCase(String nombreProducto);

    List<Producto> findByActivoTrue();

    List<Producto> findTop5ByOrderByFechaAltaDesc();

    boolean existsByCategoria_IdCategoria(Long idCategoria);

    List<Producto> findByFechaAltaBetween(LocalDate desde, LocalDate hasta);

    List<Producto> findByFechaAltaAfter(LocalDate desde);

    /** 🔹 Filtro combinado flexible - EXCLUYENDO la imagen de operaciones LOWER */
    @Query(value = "SELECT p FROM Producto p " +
                   "LEFT JOIN p.categoria c " +
                   "WHERE (:nombre IS NULL OR LOWER(p.nombreProducto) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
                   "AND (:categoriaId IS NULL OR c.idCategoria = :categoriaId) " +
                   "AND (:activo IS NULL OR p.activo = :activo) " +
                   "AND (:fechaDesde IS NULL OR p.fechaAlta >= :fechaDesde) " +
                   "AND (:fechaHasta IS NULL OR p.fechaAlta <= :fechaHasta)")
    List<Producto> filtrarProductos(
            @Param("nombre") String nombre,
            @Param("categoriaId") Long categoriaId,
            @Param("activo") Boolean activo,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta
    );
}