package com.masterserv.productos.repository;

import com.masterserv.productos.dto.reporte.StockInmovilizadoDTO;
import com.masterserv.productos.dto.reporte.ValorizacionInventarioDTO;
import com.masterserv.productos.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    Optional<Producto> findByNombre(String nombre);

    // --- MENTOR: NUEVO MÉTODO PARA EL GENERADOR ---
    Optional<Producto> findTopByCodigoStartingWithOrderByCodigoDesc(String prefix);
    // ----------------------------------------------

    @Query("SELECT p FROM Producto p " +
            "JOIN p.categoria c " +
            "JOIN c.proveedores provs " + 
            "WHERE provs.id = :proveedorId AND p.estado = 'ACTIVO' " +
            "ORDER BY p.nombre ASC")
    List<Producto> findActivosByProveedorId(@Param("proveedorId") Long proveedorId);

    @Query("SELECT p FROM Producto p " +
            "JOIN p.categoria c " +
            "JOIN c.proveedores provs " +
            "WHERE provs.id = :proveedorId AND p.estado = 'ACTIVO' " +
            "AND (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
            " OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :search, '%')) )"
            )
    Page<Producto> searchByProveedor(
        @Param("proveedorId") Long proveedorId, 
        @Param("search") String search, 
        Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.estado = 'ACTIVO'")
    long countProductosBajoStock();

    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.estado = 'ACTIVO'")
    List<Producto> findProductosConStockBajo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Producto p WHERE p.nombre ILIKE %:termino% AND p.estado = 'ACTIVO'")
    List<Producto> findByNombreILike(@Param("termino") String termino, Pageable pageable);

    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    // --- BÚSQUEDA DEFINITIVA (Flexible: Sin acentos, Sin mayúsculas, Código+Nombre+Desc) ---
    @Query(value = """
        SELECT * FROM productos p 
        WHERE p.estado = 'ACTIVO' 
        AND (
            unaccent(p.nombre) ILIKE unaccent(concat('%', :termino, '%')) 
            OR 
            unaccent(p.descripcion) ILIKE unaccent(concat('%', :termino, '%'))
            OR 
            unaccent(p.codigo) ILIKE unaccent(concat('%', :termino, '%'))
        )
        """,
        countQuery = """
        SELECT count(*) FROM productos p 
        WHERE p.estado = 'ACTIVO' 
        AND (
            unaccent(p.nombre) ILIKE unaccent(concat('%', :termino, '%')) 
            OR 
            unaccent(p.descripcion) ILIKE unaccent(concat('%', :termino, '%'))
            OR 
            unaccent(p.codigo) ILIKE unaccent(concat('%', :termino, '%'))
        )
        """,
        nativeQuery = true)
    Page<Producto> buscarFlexible(@Param("termino") String termino, Pageable pageable);


    // --- VALIDACIÓN DE DUPLICADOS ---
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
    // ----------------------------------------


    // 1. VALORIZACIÓN DE INVENTARIO (Por Categoría)
@Query("SELECT c.nombre AS categoria, " +
       "SUM(p.stockActual) AS cantidadUnidades, " +
       "SUM(CAST(p.stockActual AS bigdecimal) * p.precioCosto) AS valorTotal " + 
       "FROM Producto p " +
       "JOIN p.categoria c " +
       "WHERE p.stockActual > 0 AND p.estado = 'ACTIVO' " +
       "GROUP BY c.nombre " +
       "ORDER BY valorTotal DESC")
List<ValorizacionInventarioDTO> obtenerValorizacionPorCategoria();

// 2. STOCK INMOVILIZADO (Productos con stock > 0 y sin ventas recientes)
// Esta es compleja: Busca productos con stock, busca su última fecha de salida,
// y filtra si la fecha es anterior al límite o si NUNCA se vendió.
@Query(value = """
        SELECT 
            p.id AS "productoId", 
            p.nombre AS "nombre", 
            c.nombre AS "categoria", 
            p.stock_actual AS "stockActual", 
            COALESCE(p.precio_costo, 0) AS "costoUnitario", 
            (p.stock_actual * COALESCE(p.precio_costo, 0)) AS "capitalParado", 
            MAX(v.fecha_creacion) AS "ultimaVenta",
            COALESCE(CAST(EXTRACT(DAY FROM (NOW() - MAX(v.fecha_creacion))) AS INTEGER), 9999) AS "diasSinVenta"
        FROM producto p 
        LEFT JOIN detalle_venta dv ON p.id = dv.producto_id 
        LEFT JOIN venta v ON dv.venta_id = v.id 
        JOIN categoria c ON p.categoria_id = c.id
        WHERE p.stock_actual > 0 AND p.estado = 'ACTIVO'
        GROUP BY p.id, p.nombre, c.nombre, p.stock_actual, p.precio_costo
        HAVING MAX(v.fecha_creacion) IS NULL OR MAX(v.fecha_creacion) < :fechaLimite
        ORDER BY "capitalParado" DESC
        """, nativeQuery = true)
    List<StockInmovilizadoDTO> obtenerStockInmovilizado(@Param("fechaLimite") java.time.LocalDateTime fechaLimite);

}