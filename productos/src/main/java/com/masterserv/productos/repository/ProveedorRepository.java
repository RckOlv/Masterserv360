package com.masterserv.productos.repository;

import com.masterserv.productos.entity.Proveedor;
import org.springframework.data.jpa.repository.EntityGraph; // Importar
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // --- MÉTODOS DE VALIDACIÓN ---
    Optional<Proveedor> findByCuit(String cuit);
    Optional<Proveedor> findByRazonSocial(String razonSocial);

    // --- MÉTODOS OPTIMIZADOS CON @EntityGraph ---
    // (Sobreescribimos los métodos estándar de JpaRepository)

    /**
     * Busca TODOS los proveedores y carga sus categorías (EAGER).
     * Override del método de JpaRepository.
     */
    @Override
    @EntityGraph(attributePaths = {"categorias"})
    List<Proveedor> findAll(); 

    /**
     * Busca proveedores por estado y carga sus categorías (EAGER).
     * Este es un método derivado VÁLIDO.
     */
    @EntityGraph(attributePaths = {"categorias"})
    List<Proveedor> findByEstado(String estado); 

    /**
     * Busca un proveedor por ID y carga sus categorías (EAGER).
     * Override del método de JpaRepository.
     */
    @Override
    @EntityGraph(attributePaths = {"categorias"})
    Optional<Proveedor> findById(Long id); 
}