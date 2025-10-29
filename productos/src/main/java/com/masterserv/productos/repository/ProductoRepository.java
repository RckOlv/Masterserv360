package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Producto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    // Método para buscar por el código de producto que definimos
    Optional<Producto> findByCodigo(String codigo);

    // Método para verificar si ya existe un producto con ese código
    boolean existsByCodigo(String codigo);

    Optional<Producto> findByNombre(String nombre);

    // Extendemos JpaSpecificationExecutor<Producto>. Esto es clave.
    // Nos permitirá construir consultas dinámicas complejas más adelante 
    // (filtrar por nombre, categoría, precio, etc.) 
    // usando el "Criteria API" y el `ProductoFiltroDTO` que tenías.

    @Query("SELECT p FROM Producto p " +
           "JOIN p.categoria c " +
           "JOIN c.proveedores provs " + // Asume 'proveedores' en Categoria.java
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
}