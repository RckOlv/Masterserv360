package com.masterserv.productos.repository;

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
}