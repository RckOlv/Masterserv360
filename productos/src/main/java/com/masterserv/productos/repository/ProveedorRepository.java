package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Proveedor;
import com.masterserv.productos.enums.EstadoUsuario; // Asegúrate de importar tu Enum
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // --- MÉTODOS DE VALIDACIÓN ---
    Optional<Proveedor> findByCuit(String cuit);
    Optional<Proveedor> findByRazonSocial(String razonSocial);

    // --- MÉTODOS OPTIMIZADOS ---

    /**
     * Busca TODOS los proveedores y carga sus categorías (EAGER).
     * Útil para listados completos de administración.
     */
    @Override
    @EntityGraph(attributePaths = {"categorias"})
    List<Proveedor> findAll(); 

    /**
     * Busca proveedores por ESTADO y carga sus categorías (EAGER).
     * Usamos el Enum EstadoUsuario para tipo seguro.
     */
    @EntityGraph(attributePaths = {"categorias"})
    List<Proveedor> findByEstado(EstadoUsuario estado); 

    /**
     * Busca un proveedor por ID y carga sus categorías.
     */
    @Override
    @EntityGraph(attributePaths = {"categorias"})
    Optional<Proveedor> findById(Long id); 

    @EntityGraph(attributePaths = {"categorias"})
    List<Proveedor> findByEstadoAndCategorias_Id(com.masterserv.productos.enums.EstadoUsuario estado, Long categoriaId);
}