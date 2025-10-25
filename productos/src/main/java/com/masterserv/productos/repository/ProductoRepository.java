package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}